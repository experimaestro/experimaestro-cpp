/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.scheduler;

import com.jcraft.jsch.*;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.arrays.ListAdaptator;
import sf.net.experimaestro.utils.log.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Persistent
public class SSHConnector implements Connector {

    static final private Logger LOGGER = Logger.getLogger();
    // SSH session
    static transient private Session session;

    @Override
    public PrintWriter printWriter(String identifier) throws JSchException, IOException {
        boolean ptimestamp = true;
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + identifier;
        final Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        OutputStream out = channel.getOutputStream();

        return new PrintWriter(out)
        {
            @Override
            public void close() {
                channel.disconnect();
                super.close();
            }
        };
    }

    public SSHConnector() throws JSchException {
        if (session == null) init();
    }

    private void init() throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession("bpiwowar", "big.lip6.fr", 22);
        jsch.setKnownHosts(new File(new File(System.getProperty("user.home"), ".ssh"), "known_hosts").getAbsolutePath());
        UserInfo ui = new MyUserInfo();
        session.setUserInfo(ui);
        session.connect();
    }

    @Override
    public int exec(String[] command, String[] envp, File workingDirectory, ArrayList<Lock> locks) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");

        final String s = Output.toString(" ", ListAdaptator.create(command), new Output.Formatter<String>() {
            public String format(String t) {
                return CommandLineTask.bashQuotes(t);
            }
        });
        channel.setCommand(s);
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);
        channel.setOutputStream(System.out);
        LOGGER.info("Starting the task [%s]", s);
        channel.connect();
        InputStream in = channel.getInputStream();
        byte[] tmp = new byte[1024];

        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                System.out.print(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                LOGGER.info("SSH exit-status: " + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                LOGGER.error(e);
                return 1;
            } finally {
                channel.disconnect();
            }
        }
        return 0;
    }

    public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
        String passwd;
        JTextField passwordField = (JTextField) new JPasswordField(20);

        MyUserInfo() {
            LOGGER.info("Created a user info");
        }

        public String getPassword() {
            LOGGER.info("Calling password");
            return passwd;
        }

        public boolean promptYesNo(String str) {
            Object[] options = {"yes", "no"};
            int foo = JOptionPane.showOptionDialog(null,
                    str,
                    "Warning",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
            return foo == 0;
        }


        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptPassword(String message) {
            Object[] ob = {passwordField};
            int result =
                    JOptionPane.showConfirmDialog(null, ob, message,
                            JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                passwd = passwordField.getText();
                return true;
            } else {
                return false;
            }
        }

        public void showMessage(String message) {
            JOptionPane.showMessageDialog(null, message);
        }

        final GridBagConstraints gbc =
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0);
        private Container panel;

        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo) {
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridx = 0;
            panel.add(new JLabel(instruction), gbc);
            gbc.gridy++;

            gbc.gridwidth = GridBagConstraints.RELATIVE;

            JTextField[] texts = new JTextField[prompt.length];
            for (int i = 0; i < prompt.length; i++) {
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridx = 0;
                gbc.weightx = 1;
                panel.add(new JLabel(prompt[i]), gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 1;
                if (echo[i]) {
                    texts[i] = new JTextField(20);
                } else {
                    texts[i] = new JPasswordField(20);
                }
                panel.add(texts[i], gbc);
                gbc.gridy++;
            }

            if (JOptionPane.showConfirmDialog(null, panel,
                    destination + ": " + name,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE)
                    == JOptionPane.OK_OPTION) {
                String[] response = new String[prompt.length];
                for (int i = 0; i < prompt.length; i++) {
                    response[i] = texts[i].getText();
                }
                return response;
            } else {
                return null;  // cancel
            }
        }
    }

}

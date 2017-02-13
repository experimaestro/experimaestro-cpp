package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Set;


public class UnixSshSeekableByteChannel implements SeekableByteChannel {
    static final long REFRESH_INTERVAL = 10; // refresh after 10 seconds
    private boolean append;
    private boolean open;
    private UnixSshPath path;
    private long position = 0;
    private UnixSshFileSystemProvider provider;
    private boolean readable;
    private PosixFileAttributes attributes;
    private boolean writeable;
    private long lastUpdate;

    public UnixSshSeekableByteChannel( UnixSshPath path, Set<? extends OpenOption> openOptions, FileAttribute<?>... createFileAttributes ) throws IOException {
        this.path = path.toAbsolutePath();
        this.append = openOptions.contains( StandardOpenOption.APPEND );
        this.readable = openOptions.isEmpty() || openOptions.contains( StandardOpenOption.READ );
        this.writeable = openOptions.contains( StandardOpenOption.WRITE );

        this.provider = path.getFileSystem().provider();

        try {
            provider.checkAccess( path );
            attributes = provider.readAttributes( path, PosixFileAttributes.class );
            lastUpdate = System.currentTimeMillis();
        }
        catch ( NoSuchFileException e ) {
        }

        boolean create = false;
        if ( openOptions.contains( StandardOpenOption.CREATE_NEW ) ) {
            if ( attributes != null ) {
                throw new FileAlreadyExistsException( path.toString() );
            }
            create = true;
        }
        else if ( openOptions.contains( StandardOpenOption.CREATE ) ) {
            if ( attributes == null ) {
                create = true;
            }
        }
        else if ( attributes == null ) {
            throw new NoSuchFileException( "file not found and no CREATE/CREATE_NEW specified for "
                    + path.toString() );
        }

        if ( create ) {
            attributes = provider.createFile( path, createFileAttributes );
        } else if (openOptions.contains( StandardOpenOption.TRUNCATE_EXISTING )) {
            truncate(0);
        }


        open = true;

        // maybe wanna lock file a la 'flock'
    }

    @Override
    public void close() throws IOException {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public UnixSshSeekableByteChannel position( long position ) throws IOException {
        this.position = position;
        return this;
    }

    @Override
    public int read( ByteBuffer bytes ) throws IOException {
        refresh();

        if ( !readable ) {
            throw new NonReadableChannelException();
        }
        if ( position >= attributes.size() ) {
            return -1;
        }

        int read = provider.read( path, position, bytes );
        position += read;
        if ( position > attributes.size() ) {
            // sucks, means somebody else is also writing this file, bad things
            // are gonna happen here...
        }

        return read;
    }

    @Override
    public long size() throws IOException {
        refresh();
        return attributes.size();
    }

    private void refresh() throws IOException {
        if (System.currentTimeMillis() > lastUpdate + REFRESH_INTERVAL) {
            attributes = provider.readAttributes( path, PosixFileAttributes.class );
            lastUpdate = System.currentTimeMillis();
        }
    }

    @Override
    public UnixSshSeekableByteChannel truncate( long size ) throws IOException {
        refresh();

        if ( !writeable ) {
            throw new NonWritableChannelException();
        }
        if ( size < 0 ) {
            throw new IllegalArgumentException( "size must be positive" );
        }
        if ( size >= attributes.size() ) {
            return this;
        }

        provider.truncateFile( path, position );
        if ( position > size ) {
            position = size;
        }

        // Force refresh next time
        lastUpdate = 0;
        return this;
    }

    @Override
    public int write( ByteBuffer bytes ) throws IOException {
        refresh();

        if ( !writeable ) {
            throw new NonWritableChannelException();
        }
        if ( append ) {
            position = size();
        }

        int written = provider.write( path, position, bytes );
        position += written;
        if ( position > attributes.size()) {
            lastUpdate = 0;
        }

        return written;
    }
}

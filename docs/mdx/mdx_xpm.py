import markdown
from markdown.preprocessors import Preprocessor
import sys
import re
import json
import pygments
from pygments.lexers import get_lexer_by_name
from pygments.formatters import HtmlFormatter

def highlight(divid, language, code):
    lexer = get_lexer_by_name("python", stripall=True)
    formatter = HtmlFormatter(linenos=False)
    return "<div id='%s'>%s</div>" % (divid, pygments.highlight(code, lexer, formatter))

re_start = re.compile(r'\[\[\[(.*)')
re_end = re.compile(r'\]\]\]')

class MyPreprocessor(Preprocessor):
    def run(self, lines):
        new_lines = []
        inside = False
        startdivid = 0
        divid = 0
        tabnames = []

        for line in lines:
            m_start = re_start.match(line)
            m_end = re_end.match(line)
            if m_start:
                if len(tabnames) > 0:
                    new_lines.append("""</div>""")
                new_lines.append("""<div id="tab_{0}">""".format(divid))
                tabnames.append((divid, m_start.group(1)))
                divid += 1
                inside=True
            elif m_end:
                s = """</div><div class='tabs'><ul>"""
                for htmlid, tabname in tabnames:
                    s += """<li><a href="#tab_{}">{}</a></li>""".format(htmlid, tabname)
                s += """</ul></div>"""
                new_lines.append(s)
                tabnames = []
            else:
                new_lines.append(line)

        return new_lines

class XPMExtension(markdown.extensions.Extension):
    # Define extension here...
    def extendMarkdown(self, md, md_globals):
        md.preprocessors["xpm"] = MyPreprocessor()

def makeExtension(**kwargs):
    return XPMExtension(**kwargs)


import markdown
from markdown.preprocessors import Preprocessor
import sys
import re

import pygments
from pygments.lexers import get_lexer_by_name
from pygments.formatters import HtmlFormatter

def highlight(divid, language, code):
    lexer = get_lexer_by_name("python", stripall=True)
    formatter = HtmlFormatter(linenos=False)
    return "<div id='%s'>%s</div>" % (divid, pygments.highlight(code, lexer, formatter))


class MyPreprocessor(Preprocessor):
    def run(self, lines):
        new_lines = []

        inside = None
        code = ""
        divid = 0


        for line in lines:
            m_start = re.compile(r'\[\[\[(.*)').match(line)
            m_end = re.compile(r'\]\]\]').match(line)
            if m_start:
                if inside is None:
                    inside = []
                    divid += 1
                    new_lines.append("<div class='tabs'><ul>")
                else:
                    inside.append(highlight(currentid, language, code))
                code = ""
                language = m_start.group(1)
                currentid = "xpmlg-%s-%s" % (divid, language)
                new_lines.append("<li><a href='#{0}'>{1}</a></li>".format(currentid, language))
            elif m_end:
                new_lines.append("</ul>")
                for l in inside:
                    new_lines.append(l)
                new_lines.append(highlight(currentid, language, code))
                new_lines.append("</div>")
                inside = None
            elif inside is not None:
                code = code + line + "\n"
            else:
                new_lines.append(line)

        return new_lines

class XPMExtension(markdown.extensions.Extension):
    # Define extension here...
    def extendMarkdown(self, md, md_globals):
        md.preprocessors["xpm"] = MyPreprocessor()

def makeExtension(**kwargs):
    return XPMExtension(**kwargs)


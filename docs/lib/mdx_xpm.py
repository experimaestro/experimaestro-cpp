import markdown
from markdown.preprocessors import Preprocessor
import sys
import re

class MyPreprocessor(Preprocessor):
    def run(self, lines):
        sys.stderr.write("processing...\n")
        new_lines = ["YES!!!"]
        for line in lines:
            m = re.compile(r'abc').match(line)
            if m:
                new_lines.append("MATCHED!!!")
                pass
            else:
                new_lines.append(line)
        return new_lines

class XPMExtension(markdown.extensions.Extension):
    # Define extension here...
    def extendMarkdown(self, md, md_globals):
        md.preprocessors["xpm"] = MyPreprocessor()

def makeExtension(**kwargs):
    return XPMExtension(**kwargs)


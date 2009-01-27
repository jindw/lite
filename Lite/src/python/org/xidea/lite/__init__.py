from Expression import evaluate
from Template import renderList
import StringIO




class Expression:
    """ generated source for ExpressionImpl

    """
    source = ""
    expression = []

    def __init__(self, el):
        self.expression = el

    def evaluate(self, context):
        if context is None:
            context = {}
        return evaluate(self.expression, context)
class Template(object):
    """ generated source for Template2

    """

    items = None

    def __init__(self, list):
        self.items = list
        
    def render(self, context, out):
        renderList(context, self.items, out)
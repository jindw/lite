package org.xidea.lite.test;


import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionFactoryImpl;
import org.xidea.lite.Template;
import org.xidea.lite.parser.CoreXMLNodeParser;

/**
 * 闭包式程序
 * @author jindw
 *
 */
public class Template1 extends Template {
        private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);

        public static final int EL_TYPE = 0;
        public static final int VAR_TYPE = 1;
        public static final int IF_TYPE = 2;
        public static final int ELSE_TYPE = 3;
        public static final int FOR_TYPE = 4;

        public static final int EL_TYPE_XML_TEXT = 6;
        public static final int ATTRIBUTE_TYPE = 7;
        public static final int IF_STRING_IN_TYPE = 8;

        public static final String FOR_KEY = "for";
        public static final String IF_KEY = "if";

        private ExpressionFactory expressionFactory = ExpressionFactoryImpl
                        .getInstance();

        public void setExpressionFactory(ExpressionFactory expressionFactory) {
                this.expressionFactory = expressionFactory;
        }

        protected List<Object> items;

        public Template1(List<Object> list) {
                this.items = this.compile2(list);
        }

        public void render(Map<? extends Object, ? extends Object > context, Writer out)
                        throws IOException {
                renderList(context, items, out);
        }

        @SuppressWarnings("unchecked")
        protected void renderList(Map context,
                        List<Object> children, Writer out) {
                for (Object item : children) {
                        try {
                                if (item instanceof TemplateItem) {
                                        ((TemplateItem) item).render(context, out);
                                } else {
                                        out.write((String) item);
                                }
                        } catch (Exception e) {//每一个指令都拒绝异常
                                if (log.isDebugEnabled()) {
                                        log.debug(e);
                                }
                        }
                }
        }

        /**
         * 编译模板数据,这里没有递归调用
         *
         * @internal
         */
        protected ArrayList<Object> compile2(List<Object> datas) {
                ArrayList<ArrayList<Object>> itemsStack = new ArrayList<ArrayList<Object>>();
                itemsStack.add(new ArrayList<Object>());
                for (int i = 0; i < datas.size(); i++) {
                        Object item = datas.get(i);
                        // alert(typeof item)
                        if (item instanceof String) {
                                pushToTop(itemsStack, item);
                        } else {
                                // alert(typeof item)
                                compileItem((Object[]) item, itemsStack);
                        }
                }
                return itemsStack.get(0);
        }

        /**
         * 模板单元编译函数
         *
         * @internal
         */
        protected void compileItem(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack) {
                if (data.length == 0) {
                        itemsStack.remove(itemsStack.size() - 1);
                        return;
                }
                switch ((Integer) data[0]) {
                case EL_TYPE:// ":el":
                        buildExpression(data, itemsStack, false);
                        break;
                case EL_TYPE_XML_TEXT:// ":el":
                        buildExpression(data, itemsStack, true);
                        break;
                case VAR_TYPE:// ":set"://var
                        buildVar(data, itemsStack);
                        break;
                case IF_TYPE:// ":if":
                        buildIf(data, itemsStack);
                        break;
                case ELSE_TYPE:// ":else-if":":else":
                        buildElse(data, itemsStack);
                        break;
                case FOR_TYPE:// ":for":
                        buildFor(data, itemsStack);
                        break;
                case ATTRIBUTE_TYPE:// ":attribute":
                        buildAttribute(data, itemsStack);
                        break;
                case IF_STRING_IN_TYPE://
                        buildIfStringIn(data, itemsStack);
                        break;
                }
        }

        public static class ForStatus {
                int index = -1;
                final int lastIndex;

                public ForStatus(int end) {
                        this.lastIndex = end - 1;
                }

                public int getIndex() {
                        return index;
                }

                public int getLastIndex() {
                        return lastIndex;
                }

        }

        protected static interface TemplateItem {
                @SuppressWarnings("unchecked")
                public void render(Map context, Writer out)
                                throws IOException;
        }

        protected Expression createExpression(Object elo) {
                return expressionFactory.createEL(elo);
        }

        @SuppressWarnings("unchecked")
        protected void printXMLAttribute(String text, Map context,
                        Writer out, boolean escapeSingleChar) throws IOException {
                for (int i = 0; i < text.length(); i++) {
                        int c = text.charAt(i);
                        switch (c) {
                        case '<':
                                out.write("&lt;");
                                break;
                        case '>':
                                out.write("&gt;");
                                break;
                        case '&':
                                out.write("&amp;");
                                break;
                        case '"':// 34
                                out.write("&#34;");
                                break;
                        case '\'':// 39
                                if (escapeSingleChar) {
                                        out.write("&#39;");
                                }
                                break;
                        default:
                                out.write(c);
                        }
                }
        }

        protected void printXMLText(String text,
                        Writer out) throws IOException {
                for (int i = 0; i < text.length(); i++) {
                        int c = text.charAt(i);
                        switch (c) {
                        case '<':
                                out.write("&lt;");
                                break;
                        case '>':
                                out.write("&gt;");
                                break;
                        case '&':
                                out.write("&amp;");
                                break;
                        default:
                                out.write(c);
                        }
                }
        }


        protected void pushToTop(ArrayList<ArrayList<Object>> itemsStack,
                        Object item) {
                itemsStack.get(itemsStack.size() - 1).add(item);
        }

        protected boolean toBoolean(Object test) {
                return test != null && !Boolean.FALSE.equals(test) && !"".equals(test);
        }

        protected void buildExpression(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack, final boolean encodeXML) {
                final Expression el = createExpression(data[1]);
                pushToTop(itemsStack, new TemplateItem() {
                        @SuppressWarnings("unchecked")
                        public void render(Map context, Writer out)
                                        throws IOException {
                                Object value = el.evaluate(context);
                                if (encodeXML && value != null) {
                                        printXMLText(String.valueOf(value), out);
                                } else {
                                        out.write(String.valueOf(value));
                                }
                        }
                });
        }

        protected void buildIf(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack) {
                final Expression el = createExpression(data[1]);
                final ArrayList<Object> children = new ArrayList<Object>();
                pushToTop(itemsStack, new TemplateItem() {
                        @SuppressWarnings("unchecked")
                        public void render(Map context, Writer out) {
                                boolean test;
                                try {
                                        test = toBoolean(el.evaluate(context));
                                        if (test) {
                                                renderList(context, children, out);
                                        }
                                } catch (Exception e) {
                                        if (log.isDebugEnabled()) {
                                                log.debug(e);
                                        }
                                        test = true;
                                }

                                // context[2] = test;//if passed(丄1�7定要放下来，确保覆盖)
                                context.put(IF_KEY, test);
                        }

                });
                itemsStack.add(children);
        }

        protected void buildIfStringIn(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack) {
                final Expression elKey = createExpression(data[1]);
                final Expression elValue = createExpression(data[2]);
                final ArrayList<Object> children = new ArrayList<Object>();
                pushToTop(itemsStack, new TemplateItem() {
                        @SuppressWarnings("unchecked")
                        public void render(Map context, Writer out) {

                                boolean test = false;
                                try {
                                        Object key = elKey.evaluate(context);
                                        Object value = elValue.evaluate(context);

                                        key = String.valueOf(key);
                                        if (value instanceof Object[]) {
                                                for (Object item : (Object[]) value) {
                                                        if (item != null
                                                                        && key.equals(String.valueOf(item))) {
                                                                test = true;
                                                                break;
                                                        }
                                                }
                                        } else if (value instanceof Collection) {
                                                for (Object item : (Collection<?>) value) {
                                                        if (item != null
                                                                        && key.equals(String.valueOf(item))) {
                                                                test = true;
                                                                break;
                                                        }
                                                }
                                        }
                                        if (test) {
                                                renderList(context, children, out);
                                        }
                                } catch (Exception e) {
                                        if (log.isDebugEnabled()) {
                                                log.debug(e);
                                        }
                                        test = true;
                                }
                                // context[2] = test;//if passed(丄1�7定要放下来，确保覆盖)
                                context.put(IF_KEY, test);
                        }

                });
                itemsStack.add(children);
        }

        protected void buildElse(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack) {
                itemsStack.remove(itemsStack.size() - 1);//
                final Expression el = data.length > 1 && data[1] == null ? null
                                : createExpression(data[1]);
                final ArrayList<Object> children = new ArrayList<Object>();
                pushToTop(itemsStack, new TemplateItem() {
                        @SuppressWarnings("unchecked")
                        public void render(Map context, Writer out) {
                                if (!toBoolean(context.get(IF_KEY))) {
                                        try {
                                                if (el == null || toBoolean(el.evaluate(context))) {// if
                                                        renderList(context, children, out);
                                                        context.put(IF_KEY, true);
                                                        ;// if passed(不用要放下去，另丄1�7分支已正帄1�7)
                                                }
                                        } catch (Exception e) {
                                                if (log.isDebugEnabled()) {
                                                        log.debug(e);
                                                }
                                                context.put(IF_KEY, true);
                                        }

                                }
                        }
                });
                itemsStack.add(children);
        }

        protected void buildFor(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack) {
                final String varName = (String) data[1];
                final Expression itemExpression = createExpression(data[2]);
                final String statusName = data.length > 3 ? (String) data[3] : null;
                final ArrayList<Object> children = new ArrayList<Object>();
                pushToTop(itemsStack, new TemplateItem() {
                        @SuppressWarnings("unchecked")
                        public void render(Map context, Writer out) {
                                Object list = itemExpression.evaluate(context);
                                List<Object> items;
                                // alert(data.constructor)
                                if (list instanceof Object[]) {
                                        items = Arrays.asList(list);
                                } else {
                                        items = (List<Object>) list;
                                }
                                int len = items.size();
                                ForStatus preiousStatus = (ForStatus) context.get(FOR_KEY);
                                try {
                                        ForStatus forStatus = new ForStatus(len);
                                        context.put(FOR_KEY, forStatus);
                                        // context.put("for", forStatus);
                                        // prepareFor(this);
                                        if (statusName != null) {
                                                context.put(statusName, forStatus);
                                        }
                                        for (Object item : items) {
                                                forStatus.index++;
                                                context.put(varName, item);
                                                renderList(context, children, out);
                                        }
                                        if (statusName != null) {
                                                context.put(statusName, preiousStatus);
                                        }
                                } finally {
                                        // context.put("for", preiousStatus);
                                        context.put(FOR_KEY, preiousStatus);// for key
                                        context.put(IF_KEY, len > 0);// if key
                                }
                        }
                });
                itemsStack.add(children);
        }

        protected void buildVar(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack) {
                final String name = (String) data[1];
                if (data.length > 1 && data[2] != null) {
                        final Expression el = createExpression(data[2]);
                        pushToTop(itemsStack, new TemplateItem() {
                                @SuppressWarnings("unchecked")
                                public void render(Map context, Writer out) {
                                        context.put(name, el.evaluate(context));
                                }
                        });
                } else {
                        final ArrayList<Object> children = new ArrayList<Object>();
                        pushToTop(itemsStack, new TemplateItem() {
                                @SuppressWarnings("unchecked")
                                public void render(Map context, Writer out) {
                                        StringWriter buf = new StringWriter();
                                        renderList(context, children, buf);
                                        context.put(name, buf.toString());
                                }
                        });
                        itemsStack.add(children);// #end
                }
        }


        protected void buildAttribute(Object[] data,
                        ArrayList<ArrayList<Object>> itemsStack) {
                final Expression el = createExpression(data[1]);
                if (data.length > 2 && data[2] != null) {
                        final String prefix = " " + data[2] + "=\"";
                        pushToTop(itemsStack, new TemplateItem() {
                                @SuppressWarnings("unchecked")
                                public void render(Map context, Writer out)
                                                throws IOException {
                                        Object result = el.evaluate(context);
                                        if (result != null) {
                                                String value;
                                                if (result instanceof String) {
                                                        value = (String) result;
                                                        if (((String) result).length() == 0) {
                                                                return;
                                                        }
                                                } else {
                                                        value = String.valueOf(result);
                                                }
                                                out.write(prefix);
                                                printXMLAttribute(value, context, out, false);
                                                out.write('"');
                                        }
                                }
                        });
                } else {
                        pushToTop(itemsStack, new TemplateItem() {
                                @SuppressWarnings("unchecked")
                                public void render(Map context, Writer out)
                                                throws IOException {
                                        Object value = el.evaluate(context);
                                        printXMLAttribute(String.valueOf(value), context, out, true);
                                }
                        });
                }

        }

}

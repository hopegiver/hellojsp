package hellojsp.util;

import java.io.*;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ExecuteDirective extends Directive {

    public String getName() {
        return "execute";
    }

    public int getType() {
        return LINE;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

        //setting default params
        String url = null;
        HttpServletRequest request = (HttpServletRequest)context.get("request");
        HttpServletResponse response = (HttpServletResponse)context.get("response");

        if(request == null || response == null) return true;

        //reading params
        if (node.jjtGetChild(0) != null) {
            url = String.valueOf(node.jjtGetChild(0).value(context));
        }

        ByteArrayOutputStream buffer = null;
        OutputStreamWriter osw = null;
        String ret = "";
        try {
            buffer = new ByteArrayOutputStream();
            osw = new OutputStreamWriter(buffer, "utf-8");
            final PrintWriter printWriter = new PrintWriter(osw);
            final HttpServletResponse wrappedResponse = new HttpServletResponseWrapper(response) {
                public PrintWriter getWriter() {
                    return printWriter;
                }
            };
            RequestDispatcher dispatcher = request.getRequestDispatcher(url);
            dispatcher.include(request, wrappedResponse);
            writer.flush();
            ret = buffer.toString();
            writer.close();
        } catch(Exception e) {
            Hello.errorLog("{ExecuteDirective.render} url:" + url, e);
        } finally {
            if(osw != null) try { osw.close(); } catch (Exception ignored) {}
            if(buffer != null) try { buffer.close(); } catch (Exception ignored) {}
        }

        //execute and write result to writer
        writer.write(ret);
        return true;

    }

}

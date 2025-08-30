package org.okapi.promql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.okapi.promql.parser.PromQLLexer;
import org.okapi.promql.parser.PromQLParser;

public class PromQLMain {

    public static void main(String[] args){
        var example = "day_of_month(timestamp(up{job=\"prometheus\"}))";
        var stream = CharStreams.fromString(example);
        var lexer = new PromQLLexer(stream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new PromQLParser(tokens);
    }
}

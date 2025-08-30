parser grammar PromQLParser;

options {
    tokenVocab = PromQLLexer;
}

expression
    : vectorOperation EOF
    ;

// Binary operations are ordered by precedence

// Unary operations have the same precedence as multiplications

vectorOperation
    : <assoc = right> vectorOperation powOp vectorOperation # vecOpPow
    | <assoc = right> vectorOperation subqueryOp            # vecOpSubQuery
    | unaryOp vectorOperation                               # vecOpUnary
    | vectorOperation multOp vectorOperation                # vecOpMult
    | vectorOperation addOp vectorOperation                 # vecOpAdd
    | vectorOperation compareOp vectorOperation             # vecOpCompare
    | vectorOperation andUnlessOp vectorOperation           # vecOpAddUnless
    | vectorOperation orOp vectorOperation                  # vecOpOr
    | vectorOperation vectorMatchOp vectorOperation         # vecOpMatch
    | vectorOperation AT vectorOperation                    # vecOpAt
    | vector                                                # vecOpvec
    ;

// Operators

unaryOp
    : (ADD | SUB)
    ;

powOp
    : POW grouping?
    ;

multOp
    : (MULT | DIV | MOD) grouping?
    ;

addOp
    : (ADD | SUB) grouping?
    ;

compareOp
    : (DEQ | NE | GT | LT | GE | LE) BOOL? grouping?
    ;

andUnlessOp
    : (AND | UNLESS) grouping?
    ;

orOp
    : OR grouping?
    ;

vectorMatchOp
    : (ON | UNLESS) grouping?
    ;

subqueryOp
    : SUBQUERY_RANGE offsetOp?
    ;

offsetOp
    : OFFSET DURATION
    ;

vector
    : function_             # vecFunc
    | aggregation           # vecAgg
    | instantSelector       # vecInstant
    | matrixSelector        # vecMatrix
    | offset                # vecOffset
    | literal               # vecLiteral
    | parens                # vecParens
    ;

parens
    : LEFT_PAREN vectorOperation RIGHT_PAREN
    ;

// Selectors

instantSelector
    : METRIC_NAME (LEFT_BRACE labelMatcherList? RIGHT_BRACE)?
    | LEFT_BRACE labelMatcherList RIGHT_BRACE
    ;

labelMatcher
    : labelName labelMatcherOperator STRING
    ;

labelMatcherOperator
    : EQ
    | NE
    | RE
    | NRE
    ;

labelMatcherList
    : labelMatcher (COMMA labelMatcher)* COMMA?
    ;

matrixSelector
    : instantSelector TIME_RANGE
    ;

offset
    : instantSelector OFFSET DURATION
    | matrixSelector OFFSET DURATION
    ;

// these are not fine-grained, certain functions are not allowed
// lexer and parsers need to be extended

function_
    : FUNCTION LEFT_PAREN (parameter (COMMA parameter)*)? RIGHT_PAREN
    ;

parameter
    : literal
    | vectorOperation
    ;

parameterList
    : LEFT_PAREN (parameter (COMMA parameter)*)? RIGHT_PAREN
    ;

// Aggregations

aggregation
    : AGGREGATION_OPERATOR parameterList
    | AGGREGATION_OPERATOR (by | without) parameterList
    | AGGREGATION_OPERATOR parameterList ( by | without)
    ;

by
    : BY labelNameList
    ;

without
    : WITHOUT labelNameList
    ;

// Vector one-to-one/one-to-many joins

grouping
    : (on_ | ignoring) (groupLeft | groupRight)?
    ;

on_
    : ON labelNameList
    ;

ignoring
    : IGNORING labelNameList
    ;

groupLeft
    : GROUP_LEFT labelNameList?
    ;

groupRight
    : GROUP_RIGHT labelNameList?
    ;

// Label names

labelName
    : keyword
    | METRIC_NAME
    | LABEL_NAME
    ;

labelNameList
    : LEFT_PAREN (labelName (COMMA labelName)*)? RIGHT_PAREN
    ;

keyword
    : AND
    | OR
    | UNLESS
    | BY
    | WITHOUT
    | ON
    | IGNORING
    | GROUP_LEFT
    | GROUP_RIGHT
    | OFFSET
    | BOOL
    | AGGREGATION_OPERATOR
    | FUNCTION
    ;

literal
    : NUMBER
    | STRING
    ;
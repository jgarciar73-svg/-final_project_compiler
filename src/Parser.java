import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private ValidationResult result;

    public SelectStatement parse(List<Token> tokens, ValidationResult result) {
        this.tokens = tokens;
        this.pos = 0;
        this.result = result;
        SelectStatement statement = new SelectStatement();
        expect(TokenType.SELECT, "SYNTACTIC_EXPECTED_SELECT");
        parseColumns(statement);
        expect(TokenType.FROM, "SYNTACTIC_EXPECTED_FROM");
        Token table = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_TABLE");
        if (table != null)
            statement.table = table.lexeme;

        // TODO SERIE 2:
        // Implementar parseo de WHERE opcional:
        // WHERE <columna> <operador> <literal> (AND|OR <columna> <operador> <literal>)*
        // Debe llenar statement.where con SourceSpan exactos.
        if (match(TokenType.WHERE)) {
            ConditionChain chain = new ConditionChain();
            WhereCondition first = parseOneCondition();
            if (first != null) {
                chain.conditions.add(first);
                while (check(TokenType.AND) || check(TokenType.OR)) {
                    String connector = current().lexeme.toUpperCase();
                    advance();
                    chain.connectors.add(connector);
                    WhereCondition next = parseOneCondition();
                    if (next != null)
                        chain.conditions.add(next);
                }
                statement.where = chain;
            }
        }
        if (check(TokenType.SEMICOLON))
            advance();
        if (!check(TokenType.EOF)) {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_UNEXPECTED_TOKEN", "Token inesperado: " + current().lexeme,
                    current().span));
        }
        return statement;
    }

    private void parseColumns(SelectStatement statement) {
        if (match(TokenType.STAR)) {
            statement.columns.add("*");
            return;
        }
        Token first = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
        if (first != null)
            statement.columns.add(first.lexeme);
        while (match(TokenType.COMMA)) {
            Token next = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
            if (next != null)
                statement.columns.add(next.lexeme);
        }
    }

    private Token expect(TokenType type, String code) {
        if (check(type))
            return advance();
        result.diagnostics
                .add(new Diagnostic(code, "Se esperaba " + type + " y se encontró " + current().type, current().span));
        return null;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return current().type == type;
    }

    private Token current() {
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private WhereCondition parseOneCondition() {
        Token col = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_WHERE_OPERAND");
        if (col == null)
            return null;
        Token op = expectOperator();
        if (op == null)
            return null;
        Token lit = expectLiteral();
        if (lit == null)
            return null;
        LiteralType litType;
        if (lit.type == TokenType.NUMBER)
            litType = LiteralType.NUMBER;
        else if (lit.type == TokenType.STRING)
            litType = LiteralType.STRING;
        else if (lit.type == TokenType.TRUE || lit.type == TokenType.FALSE)
            litType = LiteralType.BOOLEAN;
        else
            litType = LiteralType.UNKNOWN;
        return new WhereCondition(col.lexeme, op.lexeme, lit.lexeme, litType, col.span, op.span, lit.span);
    }

    private Token expectOperator() {
        TokenType t = current().type;
        if (t == TokenType.EQUAL || t == TokenType.NOT_EQUAL
                || t == TokenType.GREATER || t == TokenType.LESS
                || t == TokenType.GREATER_EQUAL || t == TokenType.LESS_EQUAL) {
            return advance();
        }
        result.diagnostics
                .add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERAND", "Se esperaba operador", current().span));
        return null;
    }

    private Token expectLiteral() {
        TokenType t = current().type;
        if (t == TokenType.NUMBER || t == TokenType.STRING
                || t == TokenType.TRUE || t == TokenType.FALSE) {
            return advance();
        }
        result.diagnostics
                .add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERAND", "Se esperaba un literal", current().span));
        return null;
    }
}

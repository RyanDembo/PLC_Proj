package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GeneratorTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        // FUN main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multi",
                        //VAR x: Integer;
                        //VAR y: Decimal;
                        //VAR z: String;
                        //FUN f(): Integer DO RETURN x; END
                        //FUN g(): Decimal DO RETURN y; END
                        //FUN h(): String DO RETURN z; END
                        //FUN main(): Integer DO END
                        new Ast.Source(
                                Arrays.asList(init(new Ast.Global("x", true, Optional.empty()), ast -> ast.setVariable(new Environment.Variable("x", "x",
                                        Environment.Type.INTEGER, true, Environment.NIL ))
                                ), init(new Ast.Global("y", true, Optional.empty()), ast -> ast.setVariable(new Environment.Variable("y", "y",
                                        Environment.Type.DECIMAL, true, Environment.NIL ))
                                ), init(new Ast.Global("z", true, Optional.empty()), ast -> ast.setVariable(new Environment.Variable("z", "z",
                                        Environment.Type.STRING, true, Environment.NIL )))),

                                Arrays.asList(init(new Ast.Function("f", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Return(init(new Ast.Expression.Access(Optional.empty(), "x"), ast -> ast.setVariable(new Environment.Variable("x","x", Environment.Type.INTEGER, true, Environment.NIL))))
                                )), ast -> ast.setFunction(new Environment.Function("f", "f", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))
                                ), init(new Ast.Function("g", Arrays.asList(), Arrays.asList(), Optional.of("Decimal"), Arrays.asList(
                                        new Ast.Statement.Return(init(new Ast.Expression.Access(Optional.empty(), "y"), ast -> ast.setVariable(new Environment.Variable("y","y", Environment.Type.DECIMAL, true, Environment.NIL))))
                                )), ast -> ast.setFunction(new Environment.Function("g", "g", Arrays.asList(), Environment.Type.DECIMAL, args -> Environment.NIL))
                                ), init(new Ast.Function("h", Arrays.asList(), Arrays.asList(), Optional.of("String"), Arrays.asList(
                                        new Ast.Statement.Return(init(new Ast.Expression.Access(Optional.empty(), "z"), ast -> ast.setVariable(new Environment.Variable("z","z", Environment.Type.STRING, true, Environment.NIL))))
                                )), ast -> ast.setFunction(new Environment.Function("h", "h", Arrays.asList(), Environment.Type.STRING, args -> Environment.NIL))),
                                 init(new Ast.Function("h", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList()
                                ), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                        )),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    int x;",
                                "    double y;",
                                "    String z;",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int f() {",
                                "        return x;",
                                "    }",
                                "",
                                "    double g() {",
                                "        return y;",
                                "    }",
                                "",
                                "    String h() {",
                                "        return z;",
                                "    }",
                                "",
                                "    int main() {",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @Test
    void testList() {
        // LIST list: Decimal = [1.0, 1.5, 2.0];
        Ast.Expression.Literal expr1 = new Ast.Expression.Literal(new BigDecimal("1.0"));
        Ast.Expression.Literal expr2 = new Ast.Expression.Literal(new BigDecimal("1.5"));
        Ast.Expression.Literal expr3 = new Ast.Expression.Literal(new BigDecimal("2.0"));
        expr1.setType(Environment.Type.DECIMAL);
        expr2.setType(Environment.Type.DECIMAL);
        expr3.setType(Environment.Type.DECIMAL);

        Ast.Global global = new Ast.Global("list", "Decimal", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(expr1, expr2, expr3))));
        Ast.Global astList = init(global, ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.DECIMAL, true, Environment.create(Arrays.asList(new Double(1.0), new Double(1.5), new Double(2.0))))));

        String expected = new String("double[] list = {1.0, 1.5, 2.0};");
        test(astList, expected);
    }

    /*
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunction(String test, Ast.Global ast, String expected) {
        test(ast, expected);
    }


    private static Stream<Arguments> testFunction() {
        List<String> param = Arrays.asList("num");
        List<String> paramT = Arrays.asList("Decimal");
        return Stream.of(
                Arguments.of("Mutable Declaration",
                        //FUN square(num: Decimal): Decimal DO
                        //RETURN num * num;
                        //END

                        init(new Ast.Function("square", param, paramT, Optional.of("Decimal"), Arrays.asList(
                             new Ast.Expression.Binary("*", init(new Ast.Expression.Access(Optional.empty(), "num")
                             , ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.DECIMAL, true, Environment.NIL))
                             ), init(new Ast.Expression.Access(Optional.empty(), "num")
                             , ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.DECIMAL, true, Environment.NIL)))))),
                                ast -> ast.setFunction(new Environment.Function("square", "square", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))
                ))
        );
    }*/

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGlobals(String test, Ast.Global ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testGlobals() {
        return Stream.of(
                Arguments.of("Mutable Declaration",
                        // VAR name: Integer;
                        init(new Ast.Global("name", true, Optional.empty()), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Mutable Double",
                        // VAR name: Decimal = 1.0;
                        init(new Ast.Global("name", true, Optional.of(
                                init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL))),
                        "double name = 1.0;"
                ),
                Arguments.of("Immutable Initialization",
                        // VAL name: Decimal = 1.0;
                        init(new Ast.Global("name", false, Optional.of(
                                init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, false, Environment.NIL))),
                        "final double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL))),
                        "double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        // IF expr DO
                        //     stmt;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(new Environment.Variable("stmt", "stmt", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt;",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        // IF expr DO
                        //     stmt1;
                        // ELSE
                        //     stmt2;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt1"), ast -> ast.setVariable(new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt2"), ast -> ast.setVariable(new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true, Environment.NIL)))))
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt1;",
                                "} else {",
                                "    stmt2;",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSwitchStatement(String test, Ast.Statement.Switch ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Switch",
                        // SWITCH letter
                        //     CASE 'y':
                        //         print("yes");
                        //         letter = 'n';
                        //     DEFAULT
                        //         print("no");
                        // END
                        new Ast.Statement.Switch(
                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(init(new Ast.Expression.Literal('y'), ast -> ast.setType(Environment.Type.CHARACTER))),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("yes"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        ),
                                                        new Ast.Statement.Assignment(
                                                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                                                init(new Ast.Expression.Literal('n'), ast -> ast.setType(Environment.Type.CHARACTER))
                                                        )
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("no"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "switch (letter) {",
                                "    case 'y':",
                                "        System.out.println(\"yes\");",
                                "        letter = 'n';",
                                "    default:",
                                "        System.out.println(\"no\");",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expression.Binary ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE && FALSE
                        init(new Ast.Expression.Binary("&&",
                                init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true && false"
                ),
                Arguments.of("Concatenation",
                        // "Ben" + 10
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING)),
                        "\"Ben\" + 10"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expression.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        init(new Ast.Expression.Function("print", Arrays.asList(
                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))),
                        "System.out.println(\"Hello, World!\")"
                )
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, String expected) {
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
       // throw new UnsupportedOperationException(); //TODO
        visit(ast.getExpression());

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        if(ast.getValue().isPresent()){
            //local variables are mutable
            //if value present, visit node (expression)
            scope.defineVariable(ast.getName(), true,
                    visit(ast.getValue().get()));
        }
        else{
            scope.defineVariable(ast.getName(), true,
                    Environment.NIL);
        }


        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(ast.getReceiver() instanceof Ast.Expression.Access){
            Environment.Variable var = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName());

            if(!var.getMutable()){
                throw new RuntimeException("Receiving variable is not Mutable.");
            }


            // should deal with index out of bounds for list
            Environment.PlcObject obj = visit(ast.getReceiver());

            if(((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()){
                // is a list
                BigInteger offset = requireType(BigInteger.class, visit(((Ast.Expression.Access) ast.getReceiver()).getOffset().get()));

                int off = offset.intValue();
                List list = (List) var.getValue().getValue();
//may be wrong
                //TEST if need to update tree node value for the access property
                list.set(off, visit(ast.getValue()).getValue());



                var.setValue(Environment.create(list));
            }
            else{
            // is mutable var
                var.setValue(visit(ast.getValue()));

            }


            return Environment.NIL;
        }
        else{
            throw new RuntimeException("Receiving variable is not Accessible.");
        }


    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        //throw new UnsupportedOperationException(); //TODO

        if(requireType(Boolean.class, visit(ast.getCondition()))){
            try{
                //new scope       //parent scope
                scope = new Scope(scope);

                for(Ast.Statement stmt : ast.getThenStatements()){
                    visit(stmt); //visits all statements


                }

            }

            finally{
                scope = scope.getParent(); // returns to parent scope
            }


        }
        else{
            try{
                //new scope       //parent scope
                scope = new Scope(scope);

                for(Ast.Statement stmt : ast.getElseStatements()){
                    visit(stmt); //visits all statements
                }

            }

            finally{
                scope = scope.getParent(); // returns to parent scope
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        while(requireType(Boolean.class, visit(ast.getCondition()))){
            try{
                //new scope       //parent scope
                scope = new Scope(scope);

                for(Ast.Statement stmt : ast.getStatements()){
                    visit(stmt); //visits all statements


                }
                //ast.getStatements().forEach(this::visit);
            }
            //finally executes after the try AND after any exceptions
            finally{
                scope = scope.getParent(); // returns to parent scope
            }


        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        //throw new UnsupportedOperationException(); //TODO

        Environment.PlcObject obj = visit(ast.getValue());

        throw new Return(obj);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO

        // may need new keyword
        if(ast.getLiteral() == null){
            return Environment.NIL;
        }
        else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        //throw new UnsupportedOperationException(); //TODO

        Environment.PlcObject obj = visit(ast.getExpression());

        return Environment.create(obj.getValue());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        //throw new UnsupportedOperationException(); //TODO

        String op = ast.getOperator();

        switch (op){
            case "&&":
                boolean result = requireType(Boolean.class, visit(ast.getLeft())) &&
                        requireType(Boolean.class, visit(ast.getRight()));
                return Environment.create(result);

            case "||":
                boolean res = requireType(Boolean.class, visit(ast.getLeft())) ||
                        requireType(Boolean.class, visit(ast.getRight()));
                return Environment.create(res);


            case "<":
                if(visit(ast.getLeft()).getValue() instanceof BigInteger){
                    int res1 = requireType(BigInteger.class, visit(ast.getLeft())).
                            compareTo(requireType(BigInteger.class, visit(ast.getRight())));

                    if(res1 == -1){
                        return Environment.create(true);
                    }
                    else{
                        return Environment.create(false);
                    }
                }
                else if(visit(ast.getLeft()).getValue() instanceof BigDecimal){
                    int res1 = requireType(BigDecimal.class, visit(ast.getLeft())).
                            compareTo(requireType(BigDecimal.class, visit(ast.getRight())));
                    if(res1 == -1){
                        return Environment.create(true);
                    }
                    else{
                        return Environment.create(false);
                    }

                }
                else{
                    throw new RuntimeException("Expected numeric type");
                }


            case ">":
                if(visit(ast.getLeft()).getValue() instanceof BigInteger){
                    int res1 = requireType(BigInteger.class, visit(ast.getLeft())).
                            compareTo(requireType(BigInteger.class, visit(ast.getRight())));

                    if(res1 == 1){
                        return Environment.create(true);
                    }
                    else{
                        return Environment.create(false);
                    }
                }
                else if(visit(ast.getLeft()).getValue() instanceof BigDecimal){
                    int res1 = requireType(BigDecimal.class, visit(ast.getLeft())).
                            compareTo(requireType(BigDecimal.class, visit(ast.getRight())));
                    if(res1 == 1){
                        return Environment.create(true);
                    }
                    else{
                        return Environment.create(false);
                    }

                }
                else{
                    throw new RuntimeException("Expected numeric type");
                }

            case "==":
                boolean res2 = visit(ast.getLeft()).equals(visit(ast.getRight()));
                return Environment.create(res2);

            case "!=":
                boolean res3 = visit(ast.getLeft()).equals(visit(ast.getRight()));
                return Environment.create(!res3);
//TEST
            case "+":
                if(visit(ast.getLeft()).getValue() instanceof String){
                    return Environment.create(visit(ast.getLeft()).getValue() + visit(ast.getRight()).getValue().toString());
                   /* if(visit(ast.getRight()).getValue() instanceof BigInteger || visit(ast.getRight()).getValue() instanceof BigDecimal ){
                        return Environment.create(visit(ast.getLeft()).getValue() + visit(ast.getRight()).getValue().toString());
                    }
                    else{
                        return Environment.create(visit(ast.getLeft()).getValue() + visit(ast.getRight()).getValue().toString();
                    } */
                }
                else if(visit(ast.getRight()).getValue() instanceof String){
                    return Environment.create(visit(ast.getLeft()).getValue().toString() + visit(ast.getRight()).getValue());
                }
                else if(visit(ast.getLeft()).getValue() instanceof BigInteger){

                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).
                            add(requireType(BigInteger.class, visit(ast.getRight()))));

                }
                else if(visit(ast.getLeft()).getValue() instanceof BigInteger){
                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).
                            add(requireType(BigDecimal.class, visit(ast.getRight()))));
                }
                else{
                    throw new RuntimeException("Expected numeric or string type");
                }

            case "-":
                if(visit(ast.getLeft()).getValue() instanceof BigInteger){
                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).
                            subtract(requireType(BigInteger.class, visit(ast.getRight()))));
                }
                else if(visit(ast.getLeft()).getValue() instanceof BigDecimal){
                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).
                            subtract(requireType(BigDecimal.class, visit(ast.getRight()))));
                }
                else{
                    throw new RuntimeException("Expected numeric types");
                }

            case "*":
                if(visit(ast.getLeft()).getValue() instanceof BigInteger){
                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).
                            multiply(requireType(BigInteger.class, visit(ast.getRight()))));
                }
                else if(visit(ast.getLeft()).getValue() instanceof BigDecimal){
                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).
                            multiply(requireType(BigDecimal.class, visit(ast.getRight()))));
                }
                else{
                    throw new RuntimeException("Expected numeric types");
                }

            case "/":
                if(visit(ast.getLeft()).getValue() instanceof BigInteger){
                    BigInteger RHS = requireType(BigInteger.class, visit(ast.getRight()));

                    BigInteger zero = BigInteger.ZERO;

                    if(RHS.compareTo(zero) == 0){
                        throw new RuntimeException("Divide by Zero");
                    }

                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).
                            divide(RHS));
                }
                else if(visit(ast.getLeft()).getValue() instanceof BigDecimal){
                    BigDecimal RHS = requireType(BigDecimal.class, visit(ast.getRight()));

                    BigDecimal zero = BigDecimal.ZERO;

                    if(RHS.compareTo(zero) == 0){
                        throw new RuntimeException("Divide by Zero");
                    }

                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).
                            divide(RHS, RoundingMode.HALF_EVEN));
                }
                else{
                    throw new RuntimeException("Expected numeric types");
                }
            case "^":
                BigInteger RHS = requireType(BigInteger.class, visit(ast.getRight()));

                if(visit(ast.getLeft()).getValue() instanceof BigInteger){
                    BigInteger LHS = (BigInteger) visit(ast.getLeft()).getValue();
                    BigInteger cumult = new BigInteger("1");
                    BigInteger one = BigInteger.ONE;
                    BigInteger zero = BigInteger.ZERO;

                    if(RHS.compareTo(zero) == 0){
                        return Environment.create(one);
                    }

                    while(RHS.compareTo(zero) != 0){
                        cumult = cumult.multiply(LHS);


                        RHS = RHS.subtract(one);
                    }

                    return Environment.create(cumult);

                }
                else if(visit(ast.getLeft()).getValue() instanceof BigDecimal){
                    BigDecimal LHS = (BigDecimal) visit(ast.getLeft()).getValue();
                    BigDecimal cumult = new BigDecimal("1");
                    BigInteger one = BigInteger.ONE;
                    BigInteger zero = BigInteger.ZERO;

                    if(RHS.compareTo(zero) == 0){
                        return Environment.create(one);
                    }

                    while(RHS.compareTo(zero) != 0){
                        cumult = cumult.multiply(LHS);


                        RHS = RHS.subtract(one);
                    }

                    return Environment.create(cumult);
                }
                else{
                    throw new RuntimeException("Exponent must be BigInteger, Base must be numeric");
                }

            default:
                throw new RuntimeException("Unexpected Binary Operator");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(ast.getOffset().isPresent()){
            //access a list

            BigInteger offset = requireType(BigInteger.class, visit(ast.getOffset().get()));

            Environment.Variable var = scope.lookupVariable(ast.getName());

            //requireType(List<Environment.PlcObject>.class, var.getValue())
            if(var.getValue().getValue() instanceof List){
                List list = (List) var.getValue().getValue();

                int size = list.size();

                BigInteger SIZE = new BigInteger(Integer.toString(size));

                BigInteger ZERO = BigInteger.ZERO;
                //making sure offset in range
                //CHECK IF NEED Exact METHOD from BigInteger
                if(offset.compareTo(ZERO) != -1 && offset.compareTo(SIZE) == -1){
                    return Environment.create(list.get(offset.intValue()));
                }
                else{
                    throw new RuntimeException("Offset out of range");
                }

            }
            else{
                throw new RuntimeException("Offset value recieved, not a list");
            }

        }
        else{

            Environment.Variable var = scope.lookupVariable(ast.getName());

            return var.getValue();
        }

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO

        int numArgs = ast.getArguments().size();
        List<Environment.PlcObject> args = new ArrayList<>();

        for(Ast.Expression exp : ast.getArguments()){
            //visit(exp); //visits all expression
            args.add(visit(exp));

        }

        //arity = 0
        Environment.Function fun = scope.lookupFunction(ast.getName(), numArgs);

        return Environment.create(fun.invoke(args).getValue());

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        //throw new UnsupportedOperationException(); //TODO
        List<Object> items = new ArrayList<>();

        for(Ast.Expression exp : ast.getValues()){
            //visit(exp); //visits all expression
            items.add(visit(exp).getValue());

        }


        //System.out.println(ast.getValues());
        return Environment.create(items);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            //returns the object tested
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}

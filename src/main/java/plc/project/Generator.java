package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("public class Main {");
        newline(indent);

        if(ast.getGlobals().size() > 0){
            newline(++indent);

            int i = 0;
            for(Ast.Global g : ast.getGlobals()){
                if(i != ast.getGlobals().size()-1) {
                    visit(g);
                    newline(indent);
                    i++;
                }
                else{
                    //last global
                    visit(g);
                    newline(--indent);
                }
            }
            newline(++indent);
        } else {
            newline(++indent);
        }


        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        newline(--indent);

        if(ast.getFunctions().size() > 0){
            newline(++indent);
        }

        int i = 0;
        for(Ast.Function f : ast.getFunctions()){
            if(i != ast.getFunctions().size()-1) {
                visit(f);
                newline(--indent);
                newline(++indent);
                i++;
            }
            else{
                //last statement
                visit(f);
                newline(--indent);
            }
        }
        indent--;
        newline(indent);
        print("}");


        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(ast.getMutable()){
            if(ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList){
                print(ast.getVariable().getType().getJvmName(), "[] ", ast.getName(), " = ", ast.getValue().get(), ";");

            } else {
                if(ast.getValue().isPresent()){
                    print(ast.getVariable().getType().getJvmName(), " ", ast.getName(), " = ", ast.getValue().get(), ";");
                } else {
                    print(ast.getVariable().getType().getJvmName(), " ", ast.getName(), ";");
                }
            }
        } else {
            print("final ", ast.getVariable().getType().getJvmName(), " ", ast.getName(), " = ", ast.getValue().get(), ";");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        String paramList = "";
        int i = 0;
        for (Environment.Type paramType : ast.getFunction().getParameterTypes()){
            if(i != ast.getParameterTypeNames().size()-1){
                paramList = paramList + paramType.getJvmName() + " " + ast.getParameters().get(i) + ", ";
            } else {
                paramList = paramList + paramType.getJvmName() + " " + ast.getParameters().get(i);
            }
            i++;
        }

        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getName(), "(", paramList, ")", " {");

        if(ast.getStatements().size() > 0){
            newline(++indent);
            i = 0;
            for(Ast.Statement s : ast.getStatements()){
                if(i != ast.getStatements().size()-1) {
                    print(s);
                    newline(indent);
                    i++;
                }
                else{
                    //last statement
                    print(s);
                    newline(--indent);
                    print("}");
                }
            }
        } else {
            newline(indent);
            print("}");
        }


        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        //throw new UnsupportedOperationException(); //TODO
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());

        if(ast.getValue().isPresent()){
            print(" = ", ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        //throw new UnsupportedOperationException(); //TODO
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        //throw new UnsupportedOperationException(); //TODO
        //HELP case if no statements inside of if statement {} or { *newline* }
        print("if (", ast.getCondition(), ") {");
        if(ast.getThenStatements().isEmpty()){
            print("}");
            newline(++indent);
        }
        else{
            newline(++indent);
            //i != ast.getArguments().size()-1
            int i =0;
            for(Ast.Statement stat : ast.getThenStatements()){
                if(i != ast.getThenStatements().size()-1) {
                    print(stat);
                    newline(indent);
                    i++;
                }
                else{
                    //last statement
                    print(stat);
                    newline(--indent);
                    print("}");
                }
            }
        } //outside then blocks, now check for else
        if(!ast.getElseStatements().isEmpty()){
            print(" else {");
            newline(++indent);
            int i =0;
            for(Ast.Statement stat : ast.getElseStatements()){
                if(i != ast.getElseStatements().size()-1) {
                    print(stat);
                    newline(indent);
                    i++;
                }
                else{
                    //last statement
                    print(stat);
                    newline(--indent);
                    print("}");
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("switch (", ast.getCondition(), ") {");
        newline(++indent);
        for (Ast.Statement.Case c : ast.getCases()){
            visit(c);
        }
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(ast.getValue().isPresent()){
            print("case ", ast.getValue().get(), ":");
            newline(++indent);

            int i = 0;
            for(Ast.Statement s : ast.getStatements()){
                if(i != ast.getStatements().size()-1) {
                    print(s);
                    newline(indent);
                    i++;
                }
                else{
                    //last statement
                    print(s);
                    newline(--indent);
                }
            }

        } else {
            print("default:");
            newline(++indent);

            int i = 0;
            for(Ast.Statement s : ast.getStatements()){
                if(i != ast.getStatements().size()-1) {
                    print(s);
                    newline(indent);
                    i++;
                }
                else{
                    //last statement
                    print(s);
                    indent -= 2;
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");
        if(!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if(i != 0){
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        Object obj = ast.getLiteral();

        if(obj == null){
            print("null");
        }
        else if(obj instanceof Character){
            print("'",obj,"'");
        }
        else if(obj instanceof String){
            print("\"",obj,"\"");
        }
        else{
            print(obj);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(!ast.getOperator().equals("^")){
            print(ast.getLeft()," ", ast.getOperator(), " ", ast.getRight());
        }
        else{
            print("Math.pow(", ast.getLeft(), ", ", ast.getRight(), ")");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(!ast.getOffset().isPresent()){
            print(ast.getVariable().getJvmName());
        }
        else{
            print(ast.getVariable().getJvmName(), "[", ast.getOffset().get(), "]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        print(ast.getFunction().getJvmName(), "(");

        if(ast.getArguments().isEmpty()){
            print(")");
        }
        else if(ast.getArguments().size() == 1){
            print(ast.getArguments().get(0), ")");
        }
        else{
            int i = 0;
            for(Ast.Expression expr : ast.getArguments()){
                if(i != ast.getArguments().size()-1) {
                    print(expr, ", ");
                    //HELP may not need a space here, documentation just says cmma separated
                    i++;
                }
                else{
                    print(expr);
                }
            }
            print(")");
        }
        //print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("{");
        if(ast.getValues().isEmpty()){
            print("}");
        }
        else if(ast.getValues().size() == 1){
            print(ast.getValues().get(0), "}");
        }
        else{
            int i = 0;
            for(Ast.Expression expr : ast.getValues()){
                if(i != ast.getValues().size()-1) {
                    print(expr, ", ");
                    i++;
                }
                else{
                    //last expression
                    print(expr);
                }
            }
            print("}");
        }
        return null;
    }

}
package compiladores.t4;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

public class Table {
    public Table.Tipos returnType;
    public enum Tipos{
        INT, REAL, CADEIA, LOGICO, INVALIDO, REG, VOID
    }

    public enum Structure{
        VAR, CONST, PROC, FUNC, TIPO
    }

    class InSymbol{
        String name;
        Tipos tipo;
        Structure structure;

        public InSymbol(String name, Tipos tipo, Structure structure){
            this.name = name;
            this.tipo = tipo;
            this.structure = structure;

        }

    }
    private HashMap<String, InSymbol> myTable;
    private HashMap<String, ArrayList<InSymbol>> typeTable;


    public Table(){
        myTable = new HashMap<>();
        typeTable = new HashMap<>();
    }

    public Table(Table.Tipos returnType){
        myTable = new HashMap<>();
        typeTable = new HashMap<>();
        this.returnType = returnType;
    }

    public void insert(String name, Tipos tipo, Structure structure){
        InSymbol input = new InSymbol(name, tipo, structure);
        myTable.put(name, input);
    }

    public void insert(InSymbol input){
        myTable.put(input.name, input);

    }

    public void insert(String tipoName, InSymbol input){
        if(typeTable.containsKey(tipoName)){
            typeTable.get(tipoName).add(input);
        }else{
            ArrayList<InSymbol> list = new ArrayList<>();
            list.add(input);
            typeTable.put(tipoName, list);
        }
    }

    public Tipos verify(String name){
        return myTable.get(name).tipo;
    }

    public boolean exists(String name){
        return myTable.containsKey(name); 
    }

    public ArrayList<InSymbol> getTypeProperties(String name){
        return typeTable.get(name);
    }
}

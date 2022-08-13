package compiladores.t4;

import java.util.LinkedList;
import java.util.List;

public class Escopo {
    private LinkedList<Table> pilha; //empilhando tabelas

    public Escopo(Table.Tipos returnType){
        pilha = new LinkedList<>();
        create(returnType);
    }

    public Escopo(Table t){
        pilha = new LinkedList<>();
        pilha.push(t);
    }

    public void create(Table.Tipos returnType){
        pilha.push(new Table(returnType));
    }

    public Table getEscopo(){
        return pilha.peek();
    }

    public List<Table> getPilha(){
        return pilha;
    }

    public void dropEscopo(){
        pilha.pop();
    }

    public boolean identExists(String name){
        for(Table escopo : pilha) {
            if(!escopo.exists(name)) {
                return true;
            }
        }
        return false;
    }
}

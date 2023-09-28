package io.nuvalence.valueitems;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SQLLines{
    private String select;
    private String from;
    private List<String> columns;
    private String where;

    public SQLLines(){
        select = "SELECT";
        columns = new ArrayList<>();
    }

    public void addColumn(String column){
        columns.add(column);
    }

    public void addWhere(String phrase){where = phrase;}

    public List<String> getOutput(){
        final var res = new ArrayList<String>();
        if(from == null){
            throw new RuntimeException("From has not been set here!");
        }

        res.add(select);
        res.add(String.join(",\n", columns));
        res.add(from);

        if(where != null){
            res.add(where);
        }

        return res;
    }
}

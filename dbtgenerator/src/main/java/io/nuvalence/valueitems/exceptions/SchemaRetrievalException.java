package io.nuvalence.valueitems.exceptions;

public class SchemaRetrievalException extends RuntimeException {
    public SchemaRetrievalException(String message, Exception e){
        super(message, e);
    }
}

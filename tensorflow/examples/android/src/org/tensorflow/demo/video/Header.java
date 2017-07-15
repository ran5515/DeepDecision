package org.tensorflow.demo.video;

public interface Header {
    String getName();
    
    String getValue();
    
    HeaderElement[] getElements() throws ParseException;
}

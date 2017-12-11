/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.Serializable;

/**
 *
 * @author samsung
 */
public class IPAddress implements Serializable {
    private Short bytes[] = new Short[4];
    private String str;

    public IPAddress(String str) {
        this.str = str;
        String[] temp = this.str.split("\\.");
        //System.out.println(temp.length);
        for(int i=0;i<4;i++)
        {
            bytes[i] = Short.parseShort(temp[i]);
        }
    }
    
    public Short[] getBytes()
    {
        return bytes;
    }
    
    public String getString()
    {
        return str;
    }

    @Override
    public String toString() {
        return str; //To change body of generated methods, choose Tools | Templates.
    }
    
    
}

/*
 * Copyright (c) 2000 jPOS.org.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *    "This product includes software developed by the jPOS project 
 *    (http://www.jpos.org/)". Alternately, this acknowledgment may 
 *    appear in the software itself, if and wherever such third-party 
 *    acknowledgments normally appear.
 *
 * 4. The names "jPOS" and "jPOS.org" must not be used to endorse 
 *    or promote products derived from this software without prior 
 *    written permission. For written permission, please contact 
 *    license@jpos.org.
 *
 * 5. Products derived from this software may not be called "jPOS",
 *    nor may "jPOS" appear in their name, without prior written
 *    permission of the jPOS project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  
 * IN NO EVENT SHALL THE JPOS PROJECT OR ITS CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS 
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the jPOS Project.  For more
 * information please see <http://www.jpos.org/>.
 */

package org.jpos.iso;

import java.io.*;
import java.util.*;
import org.jpos.util.Loggeable;
import org.jpos.util.LogSource;
import org.jpos.iso.packager.XMLPackager;
import org.jpos.iso.packager.ISO93BPackager;

/**
 * implements <b>Composite</b>
 * whithin a <b>Composite pattern</b>
 *
 * @author apr@cs.com.uy
 * @version $Id$
 * @see ISOComponent
 * @see ISOField
 */
public class ISOMsg extends ISOComponent 
    implements Cloneable, Loggeable, Externalizable
{
    protected Hashtable fields;
    protected int maxField;
    protected ISOPackager packager;
    protected boolean dirty, maxFieldDirty;
    protected int direction;
    protected byte[] header;
    protected int fieldNumber = -1;
    public static final int INCOMING = 1;
    public static final int OUTGOING = 2;
    private static final long serialVersionUID = 4306251831901413975L;

    public ISOMsg () {
        fields = new Hashtable ();
        maxField = -1;
        dirty = true;
        maxFieldDirty=true;
        direction = 0;
        header = null;
    }
    public ISOMsg (int fieldNumber) {
        this();
        this.fieldNumber = fieldNumber;
    }
    /**
     * Sets the direction information related to this message
     * @param direction can be either ISOMsg.INCOMING or ISOMsg.OUTGOING
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }
    /**
     * Sets an optional message header image
     * @param b header image
     */
     public void setHeader(byte[] b) {
        header = b;
     }
    /**
     * get optional message header image
     * @return message header image (may be null)
     */
     public byte[] getHeader() {
        return header;
     }
    /**
     * @return the direction (ISOMsg.INCOMING or ISOMsg.OUTGOING)
     * @see ISOChannel
     */
    public int getDirection() {
        return direction;
    }
    /**
     * @return true if this message is an incoming message
     * @see ISOChannel
     */
    public boolean isIncoming() {
        return direction == INCOMING;
    }
    /**
     * @return true if this message is an outgoing message
     * @see ISOChannel
     */
    public boolean isOutgoing() {
        return direction == OUTGOING;
    }
    /**
     * @return the max field number associated with this message
     */
    public int getMaxField() {
        if (maxFieldDirty)
            recalcMaxField();
        return maxField;
    }
    private void recalcMaxField() {
        ISOComponent c;
        maxField = 0;
        for (int i=1; i<=192; i++)
            if ((c = (ISOComponent) fields.get (new Integer (i))) != null)
                maxField = i;
        maxFieldDirty = false;
    }
    /**
     * @param p - a peer packager
     */
    public void setPackager (ISOPackager p) {
        packager = p;
    }
    /**
     * @return the peer packager
     */
    public ISOPackager getPackager () {
        return packager;
    }
    /**
     * Set a field within this message
     * @param c - a component
     * @exception ISOException
     */
    public void set (ISOComponent c) throws ISOException {
        Integer i = (Integer) c.getKey();
        fields.put (i, c);
        if (i.intValue() > maxField)
            maxField = i.intValue();
        dirty = true;
    }
    /**
     * Unset a field if it exists, otherwise ignore.
     * @param fldno - the field number
     */
    public void unset (int fldno) {
        if (fields.remove (new Integer (fldno)) != null)
	    dirty = maxFieldDirty = true;
    }
    /**
     * In order to interchange <b>Composites</b> and <b>Leafs</b> we use
     * getComposite(). A <b>Composite component</b> returns itself and
     * a Leaf returns null.
     *
     * @return ISOComponent
     */
    public ISOComponent getComposite() {
        return this;
    }
    /**
     * setup BitMap
     * @exception ISOException
     */
    public void recalcBitMap () throws ISOException {
        ISOComponent c;
        if (!dirty)
            return;

	if(maxField>128)
	{
	    BitSet bmap=new BitSet(64);
	    for (int i=1; i<=64; i++)
		if((c=(ISOComponent) fields.get(new Integer (i+128))) != null) 
		    bmap.set (i);
	    set (new ISOBitMap (65, bmap));
	}
		
        BitSet bmap = new BitSet (getMaxField() > 64 ? 128 : 64);
		int tmpMaxField=maxField > 128 ? 128 : maxField;

        for (int i=1; i<=tmpMaxField; i++)
            if ((c = (ISOComponent) fields.get (new Integer (i))) != null) 
                bmap.set (i);
        set (new ISOBitMap (-1, bmap));
		
        dirty = false;
    }
    /**
     * clone fields
     */
    public Hashtable getChildren() {
        return (Hashtable) fields.clone();
    }
    /**
     * pack the message with the current packager
     * @return the packed message
     * @exception ISOException
     */
    public byte[] pack() throws ISOException {
        synchronized (this) {
            recalcBitMap();
            return packager.pack(this);
        }
    }
    /**
     * unpack a message
     * @param b - raw message
     * @return consumed bytes
     * @exception ISOException
     */
    public int unpack(byte[] b) throws ISOException {
        synchronized (this) {
            return packager.unpack(this, b);
        }
    }
    /**
     * dump the message to a PrintStream. The output is sorta
     * XML, intended to be easily parsed.
     * <br>
     * Each component is responsible for its own dump function,
     * ISOMsg just calls dump on every valid field.
     * @param p - print stream
     * @param indent - optional indent string
     */
    public void dump (PrintStream p, String indent) {
        ISOComponent c;
	p.print (indent + "<" + XMLPackager.ISOMSG_TAG);
	switch (direction) {
	    case INCOMING:
		p.print (" direction=\"incoming\"");
		break;
	    case OUTGOING:
		p.print (" direction=\"outgoing\"");
		break;
	}
	if (fieldNumber != -1)
	    p.print (" "+XMLPackager.ID_ATTR +"=\""+fieldNumber +"\"");
	p.println (">");
	String newIndent = indent + "  ";
        for (int i=0; i<=maxField; i++) {
            if ((c = (ISOComponent) fields.get (new Integer (i))) != null)
                c.dump (p, newIndent);
	    //
	    // Uncomment to include bitmaps within logs
	    // 
	    // if (i == 0) {
	    //  if ((c = (ISOComponent) fields.get (new Integer (-1))) != null)
	    //    c.dump (p, newIndent);
	    // }
	    //
	}

        p.println (indent + "</" + XMLPackager.ISOMSG_TAG+">");
    }
    /**
     * get the component associated with the given field number
     * @param fldno the Field Number
     * @return the Component
     */
    public ISOComponent getComponent(int fldno) {
        return (ISOComponent) fields.get(new Integer(fldno));
    }
    /**
     * Return the object value associated with the given field number
     * @param fldno the Field Number
     * @return the field Object
     */
    public Object getValue(int fldno) throws ISOException {
        return getComponent(fldno).getValue();
    }
    /**
     * Return the String value associated with the given ISOField number
     * @param fldno the Field Number
     * @return field's String value
     */
    public String getString (int fldno) {
        String s = null;
        try {
            Object obj = getValue(fldno);
            if (obj instanceof String)
                s = (String) obj;
        } catch (ISOException e) {
            // ignore ISOException - return null
        }
        return s;
    }
    /**
     * Check if a given field is present
     * @param fldno the Field Number
     * @return boolean indicating the existence of the field
     */
    public boolean hasField(int fldno) {
        return fields.get(new Integer(fldno)) != null;
    }
    /**
     * Check if all fields are present
     * @param fields an array of fields to check for presence
     * @return true if all fields are present
     */
    public boolean hasFields (int[] fields) {
	for (int i=0; i<fields.length; i++) 
            if (!hasField (fields[i]))
                return false;
        return true;
    }
    /**
     * Don't call setValue on an ISOMsg. You'll sure get
     * an ISOException. It's intended to be used on Leafs
     * @see ISOField
     * @see ISOException
     */
    public void setValue(Object obj) throws ISOException {
        throw new ISOException ("setValue N/A in ISOMsg");
    }
    
    public Object clone() {
        try {
            ISOMsg m = (ISOMsg) super.clone();
            m.fields = (Hashtable) fields.clone();
            return (Object) m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Partially clone an ISOMsg
     * @param fields int array of fields to go
     * @return new ISOMsg instance
     */
    public Object clone(int[] fields) {
        try {
            ISOMsg m = (ISOMsg) super.clone();
            m.fields = new Hashtable();
            for (int i=0; i<fields.length; i++) {
                if (hasField(fields[i])) {
                    try {
                        m.set (getComponent(fields[i]));
                    } catch (ISOException e) { 
                        // it should never happen
                    }
                }
            }
            return (Object) m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * add all fields present on received parameter to this ISOMsg<br>
     * please note that received fields take precedence over 
     * existing ones (simplifying card agent message creation 
     * and template handling)
     * @param m ISOMsg to merge
     */
    public void merge (ISOMsg m) {
	for (int i=0; i<=m.getMaxField(); i++) 
	    try {
		if (m.hasField(i))
		    set (m.getComponent(i));
	    } catch (ISOException e) {
		// should never happen 
	    }
    }

    /**
     * @return a string suitable for a log
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        if (isIncoming())
            s.append("<-- ");
        else if (isOutgoing())
            s.append("--> ");
        else
            s.append("    ");

        try {
            s.append((String) getValue(0));
            if (hasField(11)) {
                s.append(' ');
                s.append((String) getValue(11));
            }
            if (hasField(41)) {
                s.append(' ');
                s.append((String) getValue(41));
            }
        } catch (ISOException e) { }
        return s.toString();
    }
    public Object getKey() throws ISOException {
        if (fieldNumber != -1)
            return new Integer(fieldNumber);
        throw new ISOException ("This is not a subField");
    }
    public Object getValue() {
        return this;
    }
    /**
     * @return true on inner messages
     */
    public boolean isInner() {
	return fieldNumber > -1;
    }
    /**
     * @param mti new MTI
     * @exception ISOException if message is inner message
     */
    public void setMTI (String mti) throws ISOException {
	if (isInner())
	    throw new ISOException ("can't setMTI on inner message");
	set (new ISOField (0, mti));
    }
    /**
     * @return current MTI
     * @exception ISOException on inner message or MTI not set
     */
    public String getMTI() throws ISOException {
	if (isInner())
	    throw new ISOException ("can't getMTI on inner message");
	else if (!hasField(0))
	    throw new ISOException ("MTI not available");
	return (String) getValue(0);
    }
    /**
     * @return true if message "seems to be" a request
     * @exception ISOException on MTI not set
     */
    public boolean isRequest() throws ISOException {
	return Character.getNumericValue(getMTI().charAt (2))%2 == 0;
    }
    /**
     * @return true if message "seems not to be" a request
     * @exception ISOException on MTI not set
     */
    public boolean isResponse() throws ISOException {
	return !isRequest();
    }
    /**
     * @return true if message is Retransmission
     * @exception ISOException on MTI not set
     */
    public boolean isRetransmission() throws ISOException {
	return getMTI().charAt(3) == '1';
    }
    /**
     * sets an appropiate response MTI<br>
     * i.e. 0110 becomes 0120<br>
     * i.e. 0111 becomes 0120<br>
     * i.e. 1201 becomes 1210<br>
     * @exception ISOException on MTI not set or it is not a request
     */
    public void setResponseMTI() throws ISOException {
	if (!isRequest())
	    throw new ISOException ("not a request - can't set response MTI");

	String mti = getMTI();
	set (new ISOField (0,
	    mti.substring(0,2)
		+(Character.getNumericValue(getMTI().charAt (2))+1) + "0"
	    )
	);
    }

    public void writeExternal (ObjectOutput out) throws IOException {
        int max = getMaxField();
        out.writeByte (0);  // reserved for future expansion (version id)
        out.writeShort (fieldNumber);
        for (int i=0; i<=max; i++) {
            ISOComponent c = getComponent (i);
            if (c instanceof ISOMsg) {
                out.writeByte ('M');
                ((Externalizable) c).writeExternal (out);
            }
            else if (c instanceof ISOBinaryField) {
                out.writeByte ('B');
                ((Externalizable) c).writeExternal (out);
            }
            else if (c instanceof ISOField) {
                out.writeByte ('F');
                ((Externalizable) c).writeExternal (out);
            }
        }
        out.writeByte ('E');
    }

    public void readExternal  (ObjectInput in) 
        throws IOException, ClassNotFoundException
    {
        in.readByte();  // ignore version for now
        fieldNumber = in.readShort();
        byte fieldType;
        int fldno;
        ISOComponent c;
        try {
            while ((fieldType = in.readByte()) != 'E') {
                switch (fieldType) {
                    case 'F':
                        c = new ISOField ();
                        break;
                    case 'B':
                        c = new ISOBinaryField ();
                        break;
                    case 'M':
                        c = new ISOMsg ();
                        break;
                    default:
                        throw new IOException ("malformed ISOMsg");
                }
                ((Externalizable)c).readExternal (in);
                set (c);
            }
        }
        catch (ISOException e) {
            throw new IOException (e.getMessage());
        }
    }
}


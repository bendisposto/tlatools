// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Wed 12 Jul 2017 at 16:10:00 PST by ian morris nieves
//      modified on Mon 30 Apr 2007 at 13:21:00 PST by lamport
//      modified on Fri Sep 22 13:18:45 PDT 2000 by yuanyu

package tlc2.value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tlc2.output.EC;
import tlc2.tool.ModelChecker;
import tlc2.tool.FingerprintException;
import tlc2.tool.EvalException;
import util.Assert;
import util.WrongInvocationException;

public class MethodValue extends OpValue implements Applicable {
  public Method md;

  /* Constructor */
  public MethodValue(Method md) { this.md = md; }

  public final byte getKind() { return METHODVALUE; }

  public final int compareTo(Object obj) {
    try {
      Assert.fail("Attempted to compare operator " + this.toString() +
      " with value:\n" + obj == null ? "null" : ppr(obj.toString()));
      return 0;       // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean equals(Object obj) {
    try {
      Assert.fail("Attempted to check equality of operator " + this.toString() +
      " with value:\n" + obj == null ? "null" : ppr(obj.toString()));
      return false;   // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean member(Value elem) {
    try {
      Assert.fail("Attempted to check if the value:\n" + elem == null ? "null" : ppr(elem.toString()) +
      "\nis an element of operator " + this.toString());
      return false;   // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean isFinite() {
    try {
      Assert.fail("Attempted to check if the operator " + this.toString() +
      " is a finite set.");
      return false;   // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value apply(Value arg, int control) {
    try {
      throw new WrongInvocationException("It is a TLC bug: Should use the other apply method.");
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value apply(Value[] args, int control) {
    try {
      Value res = null;
      try
      {
          res = (Value)this.md.invoke(null, (Object[]) args);
      } catch (Exception e)
      {
          if (e instanceof InvocationTargetException)
          {
              Throwable targetException = ((InvocationTargetException)e).getTargetException();
              throw new EvalException(EC.TLC_MODULE_VALUE_JAVA_METHOD_OVERRIDE, new String[]{this.md.toString(), targetException.getMessage()});
          } else
          {
              Assert.fail(EC.TLC_MODULE_VALUE_JAVA_METHOD_OVERRIDE, new String[]{this.md.toString(), e.getMessage()});
          }
      }
      return res;
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value select(Value arg) {
    try {
      throw new WrongInvocationException("It is a TLC bug: Attempted to call MethodValue.select().");
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value takeExcept(ValueExcept ex) {
    try {
      Assert.fail("Attempted to appy EXCEPT construct to the operator " +
      this.toString() + ".");
      return null;   // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value takeExcept(ValueExcept[] exs) {
    try {
      Assert.fail("Attempted to apply EXCEPT construct to the operator " +
      this.toString() + ".");
      return null;   // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value getDomain() {
    try {
      Assert.fail("Attempted to compute the domain of the operator " +
      this.toString() + ".");
      return EmptySet;   // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final int size() {
    try {
      Assert.fail("Attempted to compute the number of elements in the operator " +
      this.toString() + ".");
      return 0;   // make compiler happy
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  /* Should never normalize an operator. */
  public final boolean isNormalized() {
    try {
      throw new WrongInvocationException("It is a TLC bug: Attempted to normalize an operator.");
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final void normalize() {
    try {
      throw new WrongInvocationException("It is a TLC bug: Attempted to normalize an operator.");
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean isDefined() { return true; }

  public final Value deepCopy() { return this; }

  public final boolean assignable(Value val) {
    try {
      throw new WrongInvocationException("It is a TLC bug: Attempted to initialize an operator.");
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  /* String representation of the value.  */
  public final StringBuffer toString(StringBuffer sb, int offset) {
    try {
      return sb.append("<Java Method: " + this.md + ">");
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

}

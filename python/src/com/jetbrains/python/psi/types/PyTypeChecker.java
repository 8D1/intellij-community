package com.jetbrains.python.psi.types;

import com.jetbrains.python.psi.PyClass;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author vlan
 */
public class PyTypeChecker {
  private PyTypeChecker() {
  }

  public static boolean match(PyType expected, PyType actual, TypeEvalContext context) {
    // TODO: subscriptable types?, module types?, etc.
    if (expected == null || actual == null) {
      return true;
    }
    if (expected instanceof PyTypeReference) {
      return match(((PyTypeReference)expected).resolve(null, context), actual, context);
    }
    if (actual instanceof PyTypeReference) {
      return match(expected, ((PyTypeReference)actual).resolve(null, context), context);
    }
    if (actual instanceof PyUnionType) {
      final List<PyType> members = ((PyUnionType)actual).getMembers();
      for (PyType m : members) {
        if (m == null) {
          return true;
        }
      }
      for (PyType m : members) {
        if (!match(expected, m, context)) {
          return false;
        }
      }
      return true;
    }
    if (expected instanceof PyUnionType) {
      for (PyType t : ((PyUnionType)expected).getMembers()) {
        if (match(t, actual, context)) {
          return true;
        }
      }
      return false;
    }
    if (expected instanceof PyClassType) {
      final PyClass c = ((PyClassType)expected).getPyClass();
      if (c != null && "object".equals(c.getName())) {
        return true;
      }
    }
    if (expected instanceof PyClassType && actual instanceof PyClassType) {
      final PyClass superClass = ((PyClassType)expected).getPyClass();
      final PyClass subClass = ((PyClassType)actual).getPyClass();
      if (expected instanceof PyCollectionType && actual instanceof PyCollectionType) {
        if (!matchClasses(superClass, subClass)) {
          return false;
        }
        final PyType superElementType = ((PyCollectionType)expected).getElementType(context);
        final PyType subElementType = ((PyCollectionType)actual).getElementType(context);
        return match(superElementType, subElementType, context);
      }
      else if (expected instanceof PyTupleType && actual instanceof PyTupleType) {
        final PyTupleType superTupleType = (PyTupleType)expected;
        final PyTupleType subTupleType = (PyTupleType)actual;
        if (superTupleType.getElementCount() != subTupleType.getElementCount()) {
          return false;
        }
        else {
          for (int i = 0; i < superTupleType.getElementCount(); i++) {
            if (!match(superTupleType.getElementType(i), subTupleType.getElementType(i), context)) {
              return false;
            }
          }
          return true;
        }
      }
      else if (matchClasses(superClass, subClass)) {
        return true;
      }
    }
    if (expected.equals(actual)) {
      return true;
    }
    final String superName = expected.getName();
    final String subName = actual.getName();
    // TODO: No inheritance check for builtin numerics at this moment
    final boolean subIsBool = "bool".equals(subName);
    final boolean subIsInt = "int".equals(subName);
    final boolean subIsLong = "long".equals(subName);
    final boolean subIsFloat = "float".equals(subName);
    if (superName == null || subName == null ||
        superName.equals(subName) ||
        ("int".equals(superName) && subIsBool) ||
        ("long".equals(superName) && (subIsBool || subIsInt)) ||
        ("float".equals(superName) && (subIsBool || subIsInt || subIsLong)) ||
        ("complex".equals(superName) && (subIsBool || subIsInt || subIsLong || subIsFloat))) {
      return true;
    }
    return false;
  }

  private static boolean matchClasses(@Nullable PyClass superClass, @Nullable PyClass subClass) {
    if (superClass == null || subClass == null || subClass.isSubclass(superClass) || PyABCUtil.isSubclass(subClass, superClass)) {
      return true;
    }
    else {
      final String superName = superClass.getName();
      return superName != null && superName.equals(subClass.getName());
    }
  }
}

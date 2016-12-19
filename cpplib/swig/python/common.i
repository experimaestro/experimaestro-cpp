// Exception handling

%exception {
   try {
      $action
   } catch (Swig::DirectorException &e) {
      SWIG_fail;
      SWIG_exception(SWIG_RuntimeError, e.what());
   } catch(std::exception &e) {
      SWIG_exception(SWIG_RuntimeError, e.what());
   }
}

%feature("director:except") {
    if ($error != NULL) {
        throw Swig::DirectorMethodException();
    }
}

package itl;

import java.util.List;

public interface ItlCallable {
    Object call(Interpreter interpreter, List<Object> arguements);
    int arity();
}

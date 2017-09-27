package org.chocosolver.parser.json;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.testng.annotations.Test;

/**
 * <p> Project: choco-json.
 *
 * @author Charles Prud'homme
 * @since 12/09/2017.
 */
public class JSONModelTest extends JSONConstraintTest{


    @Test(groups = "1s")
    public void test1() {
        Model model = new Model("Gson");
        IntVar X = model.intVar("X", 2, 8);
        model.arithm(X, ">", 3).post();
        model.arithm(X, "<", 5).reify();
        model.setObjective(true, X);
        model.addHook("description", "A very short description.");
        eval(model, false);
    }
}
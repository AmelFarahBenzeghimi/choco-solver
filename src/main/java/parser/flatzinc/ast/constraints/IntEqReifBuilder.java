/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package parser.flatzinc.ast.constraints;

import parser.flatzinc.ParserConfiguration;
import parser.flatzinc.ast.Datas;
import parser.flatzinc.ast.expression.EAnnotation;
import parser.flatzinc.ast.expression.Expression;
import solver.Solver;
import solver.constraints.*;
import solver.exception.ContradictionException;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.variables.Variable;
import util.ESat;

import java.util.List;

/**
 * (a = b) &#8660; r
 * <br/>
 *
 * @author Charles Prud'homme, Jean-Guillaume Fages
 * @since 26/01/11
 */
public class IntEqReifBuilder implements IBuilder {

    @Override
    public void build(Solver solver, String name, List<Expression> exps, List<EAnnotation> annotations, Datas datas) {
        IntVar a = exps.get(0).intVarValue(solver);
        IntVar b = exps.get(1).intVarValue(solver);
        final BoolVar r = exps.get(2).boolVarValue(solver);
        // this constraint is not poster, hence not returned, because it is reified
        if (ParserConfiguration.ENABLE_CLAUSE
                && ((a.getTypeAndKind() & Variable.KIND) == Variable.BOOL) && ((b.getTypeAndKind() & Variable.KIND) == Variable.BOOL)) {
            SatFactory.addBoolIsEqVar((BoolVar) a, (BoolVar) b, r);
        } else {
            if (ParserConfiguration.HACK_REIFICATION) {
                if (a.isInstantiated() || b.isInstantiated()) {
                    IntVar x;
                    int c;
                    if (a.isInstantiated()) {
                        x = b;
                        c = a.getValue();
                    } else {
                        x = a;
                        c = b.getValue();
                    }
                    final IntVar var = x;
                    final int cste = c;
                    solver.post(new Constraint("reif(a=cste,r)", new Propagator<IntVar>(new IntVar[]{x, r}, PropagatorPriority.BINARY, false) {
                        @Override
                        public void propagate(int evtmask) throws ContradictionException {
                            if (r.getLB() == 1) {
                                var.instantiateTo(cste, aCause);
                                setPassive();
                            } else {
                                if (r.getUB() == 0) {
                                    if (var.removeValue(cste, aCause) || !var.contains(cste)) {
                                        setPassive();
                                    }
                                } else {
                                    if (var.isInstantiatedTo(cste)) {
                                        r.setToTrue(aCause);
                                        setPassive();
                                    } else if (!var.contains(cste)) {
                                        r.setToFalse(aCause);
                                        setPassive();
                                    }
                                }
                            }
                        }

                        @Override
                        public ESat isEntailed() {
//                            throw new UnsupportedOperationException("isEntailed not implemented ");
                            return ESat.TRUE;
                        }
                    }));
                } else {
                    solver.post(new Constraint("reif(a=b,r)", new Propagator<IntVar>(new IntVar[]{a, b, r}, PropagatorPriority.TERNARY, false) {
                        @Override
                        public void propagate(int evtmask) throws ContradictionException {
                            if (r.getLB() == 1) {
                                if (vars[0].isInstantiated()) {
                                    setPassive();
                                    vars[1].instantiateTo(vars[0].getValue(), aCause);
                                } else if (vars[1].isInstantiated()) {
                                    setPassive();
                                    vars[0].instantiateTo(vars[1].getValue(), aCause);
                                }
                            } else if (r.getUB() == 0) {
                                if (vars[0].isInstantiated()) {
                                    if (vars[1].removeValue(vars[0].getValue(), aCause)) {
                                        setPassive();
                                    }
                                } else if (vars[1].isInstantiated()) {
                                    if (vars[0].removeValue(vars[1].getValue(), aCause)) {
                                        setPassive();
                                    }
                                }
                            } else {
                                if (vars[0].isInstantiated()) {
                                    if (vars[1].isInstantiated()) {
                                        if (vars[0].getValue() == vars[1].getValue()) {
                                            r.setToTrue(aCause);
                                        } else {
                                            r.setToFalse(aCause);
                                        }
                                        setPassive();
                                    } else {
                                        if (!vars[1].contains(vars[0].getValue())) {
                                            setPassive();
                                            r.setToFalse(aCause);
                                        }
                                    }
                                } else {
                                    if (vars[1].isInstantiated()) {
                                        if (!vars[0].contains(vars[1].getValue())) {
                                            setPassive();
                                            r.setToFalse(aCause);
                                        }
                                    } else {
                                        if (vars[0].getLB() > vars[1].getUB()
                                                || vars[1].getLB() > vars[0].getUB()) {
                                            setPassive();
                                            r.setToFalse(aCause);
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public ESat isEntailed() {
                            throw new UnsupportedOperationException("isEntailed not implemented ");
                        }
                    }));
                }
            } else {
                ICF.arithm(a, "=", b).reifyWith(r);
            }
        }
    }
}

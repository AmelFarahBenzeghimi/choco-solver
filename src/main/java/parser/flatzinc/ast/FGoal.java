/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package parser.flatzinc.ast;

import org.slf4j.LoggerFactory;
import parser.flatzinc.FZNException;
import parser.flatzinc.ast.expression.EAnnotation;
import parser.flatzinc.ast.expression.EArray;
import parser.flatzinc.ast.expression.EIdentifier;
import parser.flatzinc.ast.expression.Expression;
import parser.flatzinc.ast.searches.IntSearch;
import parser.flatzinc.ast.searches.VarChoice;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.exception.ContradictionException;
import solver.objective.ObjectiveManager;
import solver.search.limits.ACounter;
import solver.search.limits.FailCounter;
import solver.search.loop.ISearchLoop;
import solver.search.loop.lns.LNSFactory;
import solver.search.loop.lns.LargeNeighborhoodSearch;
import solver.search.loop.monitors.SearchMonitorFactory;
import solver.search.strategy.ISF;
import solver.search.strategy.decision.Decision;
import solver.search.strategy.selectors.values.IntDomainMin;
import solver.search.strategy.selectors.variables.ActivityBased;
import solver.search.strategy.selectors.variables.DomOverWDeg;
import solver.search.strategy.selectors.variables.FirstFail;
import solver.search.strategy.selectors.variables.ImpactBased;
import solver.search.strategy.strategy.AbstractStrategy;
import solver.search.strategy.strategy.IntStrategy;
import solver.search.strategy.strategy.StrategiesSequencer;
import solver.variables.IntVar;
import solver.variables.Variable;
import util.tools.ArrayUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/*
* User : CPRUDHOM
* Mail : cprudhom(a)emn.fr
* Date : 12 janv. 2010
* Since : Choco 2.1.1
*
* Class for solve goals definition based on flatzinc-like objects.
*
* A solve goal is defined with:
* </br> 'solve annotations satisfy;'
* </br> or 'solve annotations maximize expression;'
* /br> or 'solve annotations minimize expression;' 
*/

public class FGoal {

    private enum Search {
        int_search,
        bool_search,
        set_search
    }

    public static void define_goal(Datas datas, Solver aSolver, List<EAnnotation> annotations, ResolutionPolicy type, Expression expr) {
        // First define solving process
        ISearchLoop search = aSolver.getSearchLoop();
        GoalConf gc = datas.goals();
        IntVar obj = null;
        if (type != ResolutionPolicy.SATISFACTION) {
            obj = expr.intVarValue(aSolver);
        }
        aSolver.set(new ObjectiveManager<IntVar, Integer>(obj, type, true));//                solver.setRestart(true);
        if (gc.timeLimit > -1) {
            SearchMonitorFactory.limitTime(aSolver, gc.timeLimit);
        }
//        aSolver.set(gc.searchPattern);

        // Then define search goal
        Variable[] vars = aSolver.getVars();
        IntVar[] ivars = new IntVar[vars.length];
        for (int i = 0; i < ivars.length; i++) {
            ivars[i] = (IntVar) vars[i];
        }
        Variable[] defdecvars = null; // defined dec vars
        StringBuilder description = new StringBuilder();
        if (annotations.size() > 0 && !gc.free) {
            AbstractStrategy strategy = null;
            if (annotations.size() > 1) {
                throw new UnsupportedOperationException("SolveGoal:: wrong annotations size");
            } else {
                EAnnotation annotation = annotations.get(0);
                if (annotation.id.value.equals("seq_search")) {
                    EArray earray = (EArray) annotation.exps.get(0);

                    AbstractStrategy[] strategies = new AbstractStrategy[earray.what.size()];
                    for (int i = 0; i < strategies.length; i++) {
                        strategies[i] = readSearchAnnotation((EAnnotation) earray.getWhat_i(i), aSolver, description);
                    }
                    strategy = new StrategiesSequencer(aSolver.getEnvironment(), strategies);
                } else {
                    strategy = readSearchAnnotation(annotation, aSolver, description);
                }
                defdecvars = strategy.vars;
//                aSolver.set(strategy);
                LoggerFactory.getLogger(FGoal.class).warn("% Fix seed");
                aSolver.set(
                        new StrategiesSequencer(aSolver.getEnvironment(),
                                strategy, makeComplementarySearch(datas)));

//                System.out.println("% t:" + gc.seed);
            }
        } else { // no strategy OR use free search
            if (gc.dec_vars) { // select same decision variables as declared in file?
                Variable[] dvars = new Variable[0];
                if (annotations.size() > 1) {
                    throw new UnsupportedOperationException("SolveGoal:: wrong annotations size");
                } else {
                    EAnnotation annotation = annotations.get(0);
                    if (annotation.id.value.equals("seq_search")) {
                        EArray earray = (EArray) annotation.exps.get(0);
                        for (int i = 0; i < earray.what.size(); i++) {
                            dvars = ArrayUtils.append(dvars, extractScope((EAnnotation) earray.getWhat_i(i), aSolver));
                        }
                    } else {
                        dvars = ArrayUtils.append(dvars, extractScope(annotation, aSolver));
                    }
                }
                if (dvars.length > 0) {
                    ivars = new IntVar[dvars.length];
                    for (int i = 0; i < dvars.length; i++) {
                        ivars[i] = (IntVar) dvars[i];
                    }
                }
                defdecvars = ivars;
            }

            LoggerFactory.getLogger(FGoal.class).warn("% No search annotation. Set default.");
            if (type == ResolutionPolicy.SATISFACTION && gc.all) {
                aSolver.set(new IntStrategy(ivars, new FirstFail(), new IntDomainMin()));
            } else {
                switch (gc.bbss) {
                    case 2:
                        description.append("ibs");
                        ImpactBased ibs = new ImpactBased(ivars, 2, 3, 10, gc.seed, false);

                        ibs.setTimeLimit(gc.timeLimit);
                        aSolver.set(ibs);
                        break;
                    case 3:
                        description.append("wdeg");
                        DomOverWDeg dwd = new DomOverWDeg(ivars, gc.seed, new IntDomainMin());
                        aSolver.set(dwd);
                        break;
                    case 4:
                        description.append("first_fail");
                        aSolver.set(new IntStrategy(ivars, new FirstFail(), new IntDomainMin()));
                        break;
                    case 1:
                    default:
                        description.append("abs");
                        ActivityBased abs = new ActivityBased(aSolver, ivars, 0.999d, 0.2d, 8, 1.1d, 1, gc.seed);
                        aSolver.set(abs);
//                        if (type != ResolutionPolicy.SATISFACTION) { // also add LNS in optimization
//                            aSolver.getSearchLoop().plugSearchMonitor(new ActivityBasedNeighborhood(aSolver, ivars, gc.seed, abs, false, ivars.length / 2));
//                        }
                        break;
                }
            }
        }
        plugLNS(aSolver, ivars, defdecvars != null ? defdecvars : ivars, gc);
        if (gc.lastConflict) {
            aSolver.set(ISF.lastConflict(aSolver, aSolver.getStrategy()));
        }
        gc.setDescription(description.toString());
    }


    private static void plugLNS(Solver solver, IntVar[] ivars, Variable[] ddvars, GoalConf gc) {
        LargeNeighborhoodSearch lns = null;
        IntVar[] dvars = new IntVar[ddvars.length];
        for (int i = 0; i < dvars.length; i++) {
            dvars[i] = (IntVar) ddvars[i];
        }
        ObjectiveManager om = solver.getObjectiveManager();
        ACounter fr = gc.fastRestart ? new FailCounter(30) : null;
        switch (gc.lns) {
            case RLNS:
                lns = LNSFactory.rlns(solver, dvars, 200, gc.seed, fr);
                if (!om.isOptimization()) {
                    lns = null;
                    solver.set(ISF.activity(dvars, 0));
                }
                break;
            case RLNS_BB:
                lns = LNSFactory.rlns(solver, ivars, 200, gc.seed, fr);
                break;
            case PGLNS:
                lns = LNSFactory.pglns(solver, dvars, 200, 100, 10, gc.seed, fr);
                if (!om.isOptimization()) {
                    lns = null;
                    solver.set(ISF.domOverWDeg(dvars, 0));
                }
                break;
            case PGLNS_BB:
                lns = LNSFactory.pglns(solver, ivars, 200, 100, 10, gc.seed, fr);
                break;
            case ELNS:
                lns = LNSFactory.elns(solver, dvars, 200, gc.seed, gc.fastRestart ? new FailCounter(30) : null, fr);
                break;
            case ELNS_BB:
                lns = LNSFactory.elns(solver, ivars, 200, gc.seed, gc.fastRestart ? new FailCounter(30) : null, fr);
                break;
            case PGELNS_BB:
                lns = LNSFactory.pgelns(solver, ivars, 200, gc.seed, 200, 100, gc.fastRestart ? new FailCounter(30) : null, fr);
                break;
            case APGELNS_BB:
                lns = LNSFactory.apgelns(solver, ivars, 200, gc.seed, 200, 100, gc.fastRestart ? new FailCounter(30) : null, fr);
                break;
        }
    }


    /**
     * Read search annotation and build corresponding strategy
     *
     * @param e      {@link parser.flatzinc.ast.expression.EAnnotation}
     * @param solver solver within the search is defined
     * @return {@code true} if a search strategy is defined
     */
    private static AbstractStrategy readSearchAnnotation(EAnnotation e, Solver solver, StringBuilder description) {
        Expression[] exps = new Expression[e.exps.size()];
        e.exps.toArray(exps);
        Search search = Search.valueOf(e.id.value);
        VarChoice vchoice = VarChoice.valueOf(((EIdentifier) exps[1]).value);
        description.append(vchoice.toString()).append(";");
        parser.flatzinc.ast.searches.Assignment assignment = parser.flatzinc.ast.searches.Assignment.valueOf(((EIdentifier) exps[2]).value);

        switch (search) {
            case int_search:
            case bool_search:
                IntVar[] scope = exps[0].toIntVarArray(solver);
                return IntSearch.build(scope, vchoice, assignment, solver);
            case set_search:
            default:
                LoggerFactory.getLogger(FGoal.class).error("Unknown search annotation " + e.toString());
                throw new FZNException();
        }
    }

    /**
     * Read search annotation and build corresponding strategy
     *
     * @param e      {@link parser.flatzinc.ast.expression.EAnnotation}
     * @param solver solver within the search is defined
     * @return {@code true} if a search strategy is defined
     */
    private static Variable[] extractScope(EAnnotation e, Solver solver) {
        Expression[] exps = new Expression[e.exps.size()];
        e.exps.toArray(exps);
        Search search = Search.valueOf(e.id.value);
        switch (search) {
            case int_search:
            case bool_search:
                return exps[0].toIntVarArray(solver);
            case set_search:
            default:
                LoggerFactory.getLogger(FGoal.class).error("Unknown search annotation " + e.toString());
                throw new FZNException();
        }
    }


    private static AbstractStrategy makeComplementarySearch(Datas datas) {
//        final IntVar[] ivars = datas.getSearchVars();
        final IntVar[] ivars = datas.getOutputVars();
        Arrays.sort(ivars, new Comparator<IntVar>() {
            @Override
            public int compare(IntVar o1, IntVar o2) {
                return o1.getDomainSize() - o2.getDomainSize();
            }
        });
//        return new Once(new InputOrder(ivars), new IntDomainMin());
        return new AbstractStrategy<IntVar>(ivars) {
            boolean created = false;
            Decision d = new Decision<IntVar>() {

                @Override
                public void apply() throws ContradictionException {
                    for (int i = 0; i < ivars.length; i++) {
                        if (!ivars[i].isInstantiated()) {
                            ivars[i].instantiateTo(ivars[i].getLB(), this);
                            ivars[i].getSolver().propagate();
                        }
                    }
                }

                @Override
                public Object getDecisionValue() {
                    return null;
                }

                @Override
                public void free() {
                    created = false;
                }

                @Override
                public String toString() {
                    StringBuilder st = new StringBuilder("(once)");
                    for (int i = 0; i < ivars.length; i++) {
                        if (!ivars[i].isInstantiated()) {
                            st.append(ivars[i]).append("=").append(ivars[i].getLB()).append(", ");
                        }
                    }
                    return st.toString();
                }
            };

            @Override
            public void init() throws ContradictionException {
            }

            @Override
            public Decision<IntVar> getDecision() {
                if (!created) {
                    created = true;
                    d.once(true);
                    return d;
                }
                return null;
            }
        };
    }
}

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

package parser.flatzinc.para;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.flatzinc.Flatzinc;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.objective.ObjectiveManager;
import solver.search.limits.FailCounter;
import solver.search.loop.lns.LNSFactory;
import solver.search.strategy.ISF;
import solver.thread.AbstractParallelMaster;
import solver.variables.IntVar;

public class ParserMaster extends AbstractParallelMaster<ParserSlave> {

    protected static final Logger LOGGER = LoggerFactory.getLogger("solver");

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    int bestVal;
    int nbSol;
    boolean closeWithSuccess;
    ResolutionPolicy policy;
    Flatzinc aPas;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public ParserMaster(int nbCores, String[] args) {
        nbCores = Math.min(nbCores, 4);
        slaves = new ParserSlave[nbCores];
        for (int i = 0; i < nbCores; i++) {
            slaves[i] = new ParserSlave(this, i, args);
            if (i == 0) {
                aPas = slaves[i].fznp;
            }
            Solver solver = slaves[i].fznp.getSolver();
            ObjectiveManager om = solver.getObjectiveManager();

            IntVar[] dvars = slaves[i].fznp.datas.getIntSearchVars();
            switch (i) {
                case 0:
                    solver.set(ISF.lastConflict(solver));
                    break;
                case 1:
                    if (!om.isOptimization()) {
                        solver.set(ISF.domOverWDeg(dvars, 0));
                    } else {
                        LNSFactory.pglns(solver, dvars, 200, 100, 10, slaves[i].fznp.seed, new FailCounter(30));
                    }
                    solver.set(ISF.lastConflict(solver));
                    break;
                case 2:
                    if (!om.isOptimization()) {
                        solver.set(ISF.domOverWDeg(dvars, 0));
                    } else {
                        LNSFactory.pglns(solver, dvars, 200, 100, 10, slaves[i].fznp.seed, new FailCounter(30));
                    }
                    solver.set(ISF.lastConflict(solver));
                    break;
                case 3:
                    // fix
                    break;
            }


            slaves[i].workInParallel();
        }

        wait = true;
        try {
            while (wait)
                mainThread.sleep(20);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    /**
     * A slave has CLOSED ITS SEARCH TREE, every one should stop!
     */
    public synchronized void wishGranted() {
        if (LOGGER.isInfoEnabled()) {
            if (nbSol == 0) {
                if (!closeWithSuccess) {
                    LOGGER.info("=====UNKNOWN=====");
                } else {
                    LOGGER.info("=====UNSATISFIABLE=====");
                }
            } else {
                if (!closeWithSuccess && (policy != null && policy != ResolutionPolicy.SATISFACTION)) {
                    LOGGER.info("=====UNBOUNDED=====");
                } else {
                    LOGGER.info("==========");
                }
            }
        }
        Number[] nbs = slaves[0].solver.getMeasures().toArray();
        nbs[0] = nbSol;
        nbs[5] = policy != ResolutionPolicy.SATISFACTION ? bestVal : 0;
        System.exit(0);
    }

    /**
     * A solution of cost val has been found
     * informs slaves that they must find better
     *
     * @param val
     * @param policy
     */
    public synchronized boolean newSol(int val, ResolutionPolicy policy) {
        this.policy = policy;
        if (nbSol == 0) {
            bestVal = val;
        }
        nbSol++;
        boolean isBetter = false;
        switch (policy) {
            case MINIMIZE:
                if (bestVal > val || nbSol == 1) {
                    bestVal = val;
                    isBetter = true;
                }
                break;
            case MAXIMIZE:
                if (bestVal < val || nbSol == 1) {
                    bestVal = val;
                    isBetter = true;
                }
                break;
            case SATISFACTION:
                bestVal = 1;
                isBetter = nbSol == 1;
                break;
        }
        if (isBetter) {
            for (int i = 0; i < slaves.length; i++) {
                if (slaves[i] != null) {
                    slaves[i].findBetterThan(val, policy);
                }
            }
        }
        return isBetter;
    }

    public synchronized void closeWithSuccess() {
        this.closeWithSuccess = true;
    }

}

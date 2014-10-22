/**
 *  Copyright (c) 1999-2014, Ecole des Mines de Nantes
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
package parser.flatzinc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.ParserConfiguration;
import parser.ParserListener;
import parser.flatzinc.ast.Datas;
import parser.flatzinc.layout.FZNLayout;
import solver.constraints.Constraint;
import solver.search.loop.monitors.SMF;

import java.util.ArrayList;

/**
 * A base listener for Flatzinc parser, dedicated to single thread resolution.
 * <br/>
 *
 * @author Charles Prud'homme
 * @version choco-parsers
 * @since 21/10/2014
 */
public class BaseFlatzincListener implements ParserListener {

    public static final Logger LOGGER = LoggerFactory.getLogger("solver");

    long creationTime;

    final Flatzinc fznparser;

    public BaseFlatzincListener(Flatzinc fznparser) {
        this.fznparser = fznparser;
    }


    @Override
    public void beforeParsingParameters() {

    }

    @Override
    public void afterParsingParameters() {
    }

    @Override
    public void beforeSolverCreation() {
        creationTime = -System.nanoTime();
    }

    @Override
    public void afterSolverCreation() {
        buildLayout(fznparser.instance, fznparser.datas);
    }

    @Override
    public void beforeParsingFile() {
        LOGGER.info("% parse instance...");
    }

    @Override
    public void afterParsingFile() {
        if (ParserConfiguration.PRINT_CONSTRAINT && LOGGER.isInfoEnabled()) {
            ArrayList<String> l = new ArrayList<>();
            LOGGER.info("% INVOLVED CONSTRAINTS (CHOCO) ");
            for (Constraint c : fznparser.getSolver().getCstrs()) {
                if (!l.contains(c.getName())) {
                    l.add(c.getName());
                    LOGGER.info("% {}", c.getName());
                }
            }
        }


    }

    @Override
    public void beforeConfiguringSearch() {

    }

    @Override
    public void afterConfiguringSearch() {

    }

    @Override
    public void beforeSolving() {
        LOGGER.info("% solve instance...");

        if (ParserConfiguration.PRINT_SEARCH) {
            SMF.log(fznparser.getSolver(), true, false);
        }
        fznparser.getSolver().getMeasures().setReadingTimeCount(creationTime + System.nanoTime());
    }

    @Override
    public void afterSolving() {

    }


    public void buildLayout(String instance, Datas datas) {
        FZNLayout fl = new FZNLayout(instance, datas.goals());
        datas.setmLayout(fl);
        fl.makeup();
    }
}

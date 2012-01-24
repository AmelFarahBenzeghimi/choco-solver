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

/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 03/10/11
 * Time: 19:56
 */

package solver.constraints.propagators.gary.tsp;

import choco.annotations.PropAnn;
import choco.kernel.ESat;
import choco.kernel.common.util.procedure.IntProcedure;
import choco.kernel.memory.IStateInt;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.propagators.GraphPropagator;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.recorders.fine.AbstractFineEventRecorder;
import solver.variables.EventType;
import solver.variables.graph.directedGraph.DirectedGraphVar;
import solver.variables.graph.graphOperations.connectivity.ConnectivityFinder;

/**
 *
 * Simple nocircuit contraint (from NoSubtour of Pesant or noCycle of Caseaux/Laburthe)
 * */
@PropAnn(tested=PropAnn.Status.BENCHMARK)
public class PropPathNoCycle<V extends DirectedGraphVar> extends GraphPropagator<V> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	DirectedGraphVar g;
	int n;
	private IntProcedure arcEnforced;
	private IStateInt[] origin,end,size;
	private int source,sink;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Ensures that graph has no circuit, with Caseaux/Laburthe/Pesant algorithm
	 * runs in O(1) per instantiation event
	 * @param graph
	 * @param constraint
	 * @param solver
	 * */
	public PropPathNoCycle(DirectedGraphVar graph, int source, int sink, Constraint<V, Propagator<V>> constraint, Solver solver) {
		super((V[]) new DirectedGraphVar[]{graph}, solver, constraint, PropagatorPriority.LINEAR);
		g = graph;
		this.n = g.getEnvelopGraph().getNbNodes();
		arcEnforced = new EnfArc(this);
		origin = new IStateInt[n];
		size = new IStateInt[n];
		end = new IStateInt[n];
		for(int i=0;i<n;i++){
			origin[i] = environment.makeInt(i);
			size[i] = environment.makeInt(1);
			end[i] = environment.makeInt(i);
		}
		this.source = source;
		this.sink   = sink;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		int j;
		for(int i=0;i<n;i++){
			end[i].set(i);
			origin[i].set(i);
			size[i].set(1);
		}
		for(int i=0;i<n;i++){
			j = g.getKernelGraph().getSuccessorsOf(i).getFirstElement();
			if(j!=-1){
				enforce(i,j);
			}
		}
	}

	@Override
	public void propagate(AbstractFineEventRecorder eventRecorder, int idxVarInProp, int mask) throws ContradictionException {
		if(ALWAYS_COARSE){
			propagate(0);return;
		}
		eventRecorder.getDeltaMonitor(g).forEach(arcEnforced, EventType.ENFORCEARC);
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		return EventType.ENFORCEARC.mask ;
	}

	@Override
	public ESat isEntailed() {
		if(g.instantiated()){
			int narcs = 0;
			for(int i=0;i<n;i++){
				narcs+=g.getEnvelopGraph().getSuccessorsOf(i).neighborhoodSize();
			}
			boolean connected = ConnectivityFinder.findCCOf(g.getEnvelopGraph()).size()==1;
			if(connected && narcs==n-1){
				return ESat.TRUE;
			}
			return ESat.FALSE;
		}
		return ESat.UNDEFINED;
	}

	private void enforce(int i, int j) throws ContradictionException {
		int last = end[j].get();
		int start = origin[i].get();
		if(origin[j].get()!=j){
			contradiction(g,"");
		}
		g.removeArc(last,start,this);
		origin[last].set(start);
		end[start].set(last);
		size[start].add(size[j].get());
		if(origin[sink].get()==source && size[source].get()!=n){
			contradiction(g,"non hamiltonian path");
		}
	}

	//***********************************************************************************
	// PROCEDURES
	//***********************************************************************************

	private class EnfArc implements IntProcedure {
		private GraphPropagator p;

		private EnfArc(GraphPropagator p){
			this.p = p;
		}
		@Override
		public void execute(int i) throws ContradictionException {
			enforce(i/n-1,i%n);
		}
	}
}

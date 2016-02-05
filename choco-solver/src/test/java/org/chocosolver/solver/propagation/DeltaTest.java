/**
 * Copyright (c) 2015, Ecole des Mines de Nantes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the <organization>.
 * 4. Neither the name of the <organization> nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chocosolver.solver.propagation;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.delta.EnumDelta;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.IntProcedure;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.chocosolver.solver.Cause.Null;
import static org.chocosolver.solver.search.strategy.IntStrategyFactory.*;
import static org.testng.Assert.assertFalse;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 23 sept. 2010
 */
public class DeltaTest {

    @Test(groups="1s", timeOut=60000)
    public void testAdd() {
        Solver sol = new Solver();
        EnumDelta d = new EnumDelta(sol.getEnvironment());
        for (int i = 1; i < 40; i++) {
            d.add(i, Cause.Null);
            Assert.assertEquals(d.size(), i);
        }
    }

    @Test(groups="1s", timeOut=60000)
    public void testEq() throws ContradictionException {
        Solver solver = new Solver();
        IntVar x = solver.intVar("X", 1, 6, false);
        IntVar y = solver.intVar("Y", 1, 6, false);

        solver.arithm(x, "=", y).post();

        solver.propagate();

        x.removeValue(4, Null);

        solver.propagate();

        assertFalse(y.contains(4));

    }

    @Test(groups="1s", timeOut=60000)
    public void testJL() {
        Solver solver = new Solver();
        final SetVar s0 = solver.setVar("s0", new int[]{}, new int[]{0, 1});
        final BoolVar b0 = solver.boolVar("b0");
        final BoolVar b1 = solver.boolVar("b1");
        final IntVar i0 = solver.boolVar("i0");
        solver.set(lexico_LB(i0));
        solver.setBoolsChanneling(new BoolVar[]{b0, b1}, s0, 0).post();
        solver.cardinality(s0, solver.intVar(0)).post();

        solver.findSolution();
        solver.getSearchLoop().reset();
        solver.findSolution();
    }


    @Test(groups="1s", timeOut=60000)
    public void testJL2() {
        for (int k = 0; k < 50; k++) {
            Solver s = new Solver();
            final IntVar i = s.intVar("i", -2, 2, false);
            final IntVar j = s.intVar("j", -2, 2, false);
            //Chatterbox.showDecisions(s);
            //Chatterbox.showSolutions(s);
            s.set(random_value(new IntVar[]{i, j}));
            new Constraint("Constraint", new PropTestDM1(i, j), new PropTestDM2(i, j)).post();
            s.findAllSolutions();
        }
    }

    @Test(groups="1s", timeOut=60000)
    public void testJL3() {
        for (int k = 0; k < 10; k++) {
            Solver s = new Solver();
            final IntVar i = s.intVar("i", -2, 2, true);
            final IntVar j = s.intVar("j", -2, 2, true);
            //Chatterbox.showDecisions(s);
            //Chatterbox.showSolutions(s);
            s.set(random_bound(new IntVar[]{i, j}));
            new Constraint("Constraint", new PropTestDM1(i, j), new PropTestDM2(i, j)).post();
            s.findAllSolutions();
        }
    }

    @Test(groups="1s", timeOut=60000)
    public void testJL4() {
        for (int k = 0; k < 10; k++) {
            Solver s = new Solver();
            final IntVar i = s.boolVar("i");
            final IntVar j = s.boolVar("j");
            //Chatterbox.showDecisions(s);
            //Chatterbox.showSolutions(s);
            s.set(random_value(new IntVar[]{i, j}));
            new Constraint("Constraint", new PropTestDM1(i, j), new PropTestDM2(i, j)).post();
            s.findAllSolutions();
        }
    }

    private static class PropTestDM1 extends Propagator<IntVar> {
        IntVar i, j;
        IIntDeltaMonitor iD;
        IIntDeltaMonitor jD;

        private PropTestDM1(IntVar i, IntVar j) {
            super(new IntVar[]{i, j}, PropagatorPriority.UNARY, true);
            this.i = i;
            this.j = j;
            iD = i.monitorDelta(this);
            jD = j.monitorDelta(this);
        }

        @Override
        public void propagate(int evtmask) throws ContradictionException {
            iD.unfreeze();
            jD.unfreeze();
        }

        @Override
        public void propagate(int idxVarInProp, int mask) throws ContradictionException {
            if (idxVarInProp == 0) {
                iD.freeze();
                iD.forEachRemVal((IntProcedure) x -> {
                    if (i.contains(x)) {
                        Assert.fail();
                    }
                });
                iD.unfreeze();
            } else {
                jD.freeze();
                jD.forEachRemVal((IntProcedure) x -> {
                    if (j.contains(x)) {
                        Assert.fail();
                    }
                });
                jD.unfreeze();
            }
        }

        @Override
        public ESat isEntailed() {
            return ESat.TRUE;
        }
    }

    private static class PropTestDM2 extends Propagator<IntVar> {
        IntVar i, j;

        private PropTestDM2(IntVar i, IntVar j) {
            super(new IntVar[]{i, j}, PropagatorPriority.UNARY, false);
            this.i = i;
            this.j = j;
        }

        @Override
        public void propagate(int evtmask) throws ContradictionException {
            if (j.isInstantiatedTo(1)) {
                i.removeValue(1, this);
            }
        }

        @Override
        public ESat isEntailed() {
            return ESat.TRUE;
        }

    }
}
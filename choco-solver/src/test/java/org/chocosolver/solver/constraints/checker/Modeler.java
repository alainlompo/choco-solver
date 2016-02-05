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
package org.chocosolver.solver.constraints.checker;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.nary.nValue.PropAtLeastNValues_AC;
import org.chocosolver.solver.constraints.nary.nValue.PropAtMostNValues_BC;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.util.objects.graphs.MultivaluedDecisionDiagram;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.copyOfRange;
import static org.chocosolver.solver.search.strategy.IntStrategyFactory.random_bound;
import static org.chocosolver.solver.search.strategy.IntStrategyFactory.random_value;
import static org.chocosolver.util.tools.ArrayUtils.append;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 1 oct. 2010
 */
public interface Modeler {

    Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters);

    String name();

    Modeler modelEqAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("EqAC_" + n);
            IntVar[] vars = new IntVar[n];
            try {
                for (int i = 0; i < vars.length; i++) {
                    vars[i] = s.intVar("v_" + i, domains[i]);
                    if (map != null) map.put(domains[i], vars[i]);
                }
            } catch (ArrayIndexOutOfBoundsException ce) {
//                System.out.printf("");
            }
            s.arithm(vars[0], "=", vars[1]).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelEqAC";
        }
    };

    Modeler modelInverseChannelingAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("InverseChannelingAC_" + n);
            IntVar[] X = new IntVar[n / 2];
            IntVar[] Y = new IntVar[n / 2];
            for (int i = 0; i < n / 2; i++) {
                X[i] = s.intVar("X_" + i, domains[i]);
                if (map != null) map.put(domains[i], X[i]);
                Y[i] = s.intVar("Y_" + i, domains[i + (n / 2)]);
                if (map != null) map.put(domains[i + (n / 2)], Y[i]);
            }
            IntVar[] allvars = append(X, Y);
            s.inverseChanneling(X, Y, 0, 0).post();
            s.set(random_value(allvars));
            return s;
        }

        @Override
        public String name() {
            return "modelInverseChannelingAC";
        }
    };

    Modeler modelInverseChannelingBounds = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("InverseChannelingBC_" + n);
            for (int i = 0; i < domains.length; i++) {
                int m = domains[i][0];
                int M = domains[i][domains[i].length - 1];
                domains[i] = new int[M - m + 1];
                for (int j = 0; j < M - m + 1; j++) {
                    domains[i][j] = j + m;
                }
            }
            IntVar[] X = new IntVar[n / 2];
            IntVar[] Y = new IntVar[n / 2];
            int off = n / 2;
            for (int i = 0; i < n / 2; i++) {
                X[i] = s.intVar("X_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], X[i]);
                Y[i] = s.intVar("Y_" + i, domains[i + off][0], domains[i + off][domains[i + off].length - 1], true);
                if (map != null) map.put(domains[i + (n / 2)], Y[i]);
            }
            IntVar[] allvars = append(X, Y);
            s.inverseChanneling(X, Y, 0, 0).post();
            s.set(random_bound(allvars));
            return s;
        }

        @Override
        public String name() {
            return "modelInverseChannelingBounds";
        }
    };

    Modeler modelNeqAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("NeqAC_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.arithm(vars[0], "!=", vars[1]).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelNeqAC";
        }
    };

    Modeler modelAllDiffAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("AllDiffAC_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.allDifferent(vars, "AC").post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelAllDiffAC";
        }
    };

    Modeler modelAllDiffBC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("AllDiffBC_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.allDifferent(vars, "BC").post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelAllDiffBC";
        }
    };

    Modeler modelGCC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("GCC_" + n);
            boolean closed = (Boolean) parameters;
            IntVar[] vars = new IntVar[n / 2];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            int[] values = new int[n / 2];
            IntVar[] cards = new IntVar[n / 2];
            for (int i = 0; i < cards.length; i++) {
                values[i] = i;
                cards[i] = s.intVar("c_" + i, domains[i + n / 2]);
                if (map != null) map.put(domains[i + n / 2], cards[i]);
            }
            s.globalCardinality(vars, values, cards, closed).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelGCC";
        }
    };

    Modeler modelTimes = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Times_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.times(vars[0], vars[1], vars[2]).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelTimes";
        }
    };

    Modeler modelAbsolute = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Absolute_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.absolute(vars[0], vars[1]).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelAbsolute";
        }
    };

    Modeler modelCountBC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Count");
            IntVar[] vars = new IntVar[n - 1];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], vars[i]);
            }
            IntVar occVar = s.intVar("ovar", domains[n - 1][0], domains[n - 1][domains[n - 1].length - 1], true);
            if (map != null) map.put(domains[n - 1], occVar);
            int[] params = (int[]) parameters;
            String ro = "=";
            switch (params[0]) {
                case 1:
                    ro = "<=";
                    break;
                case 2:
                    ro = ">=";
                    break;
            }
            IntVar tmp = s.intVar("occ", 0, vars.length, true);
            s.arithm(tmp, ro, occVar).post();
            s.count(params[1], vars, tmp).post();
            s.set(ISF.random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelCountBC";
        }
    };

    Modeler modelCountAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Count");
            IntVar[] vars = new IntVar[n - 1];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            IntVar occVar = s.intVar("ovar", domains[n - 1]);
            if (map != null) map.put(domains[n - 1], occVar);
            int[] params = (int[]) parameters;
            String ro = "=";
            switch (params[0]) {
                case 1:
                    ro = "<=";
                    break;
                case 2:
                    ro = ">=";
                    break;
            }
            IntVar tmp = s.intVar("occ", 0, vars.length, true);
            s.count(params[1], vars, tmp).post();
            s.arithm(tmp, ro, occVar).post();
            s.set(ISF.random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelCountAC";
        }
    };

    Modeler modelLexAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Lex");
            IntVar[] X = new IntVar[n / 2];
            for (int i = 0; i < n / 2; i++) {
                X[i] = s.intVar("X_" + i, domains[i]);
                if (map != null) map.put(domains[i], X[i]);
            }
            IntVar[] Y = new IntVar[n / 2];
            for (int i = n / 2; i < n; i++) {
                Y[i - n / 2] = s.intVar("Y_" + i, domains[i]);
                if (map != null) map.put(domains[i], Y[i - n / 2]);
            }
            Constraint ctr = (Boolean) parameters ? s.lexLess(X, Y) : s.lexLessEq(X, Y);
            ctr.post();
            s.set(random_value(append(X, Y)));
            return s;
        }

        @Override
        public String name() {
            return "modelLexAC";
        }
    };

    Modeler modelLexChainAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("LexChain");
            IntVar[] X = new IntVar[n / 3];
            for (int i = 0; i < n / 3; i++) {
                X[i] = s.intVar("X_" + i, domains[i]);
                if (map != null) map.put(domains[i], X[i]);
            }
            IntVar[] Y = new IntVar[n / 3];
            for (int i = n / 3; i < 2 * n / 3; i++) {
                Y[i - n / 3] = s.intVar("Y_" + i, domains[i]);
                if (map != null) map.put(domains[i], Y[i - n / 3]);
            }
            IntVar[] Z = new IntVar[n / 3];
            for (int i = 2 * n / 3; i < n; i++) {
                Z[i - 2 * n / 3] = s.intVar("Z_" + i, domains[i]);
                if (map != null) map.put(domains[i], Z[i - 2 * n / 3]);
            }
            Constraint ctr = (Boolean) parameters ? s.lexChainLess(X, Y, Z) : s.lexChainLessEq(X, Y, Z);
            ctr.post();
            s.set(random_value(append(X, Y, Z)));
            return s;
        }

        @Override
        public String name() {
            return "modelLexChainAC";
        }
    };

    Modeler modelNthBC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Element_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.element(vars[0], new int[]{-2, 0, 1, -1, 0, 4}, vars[1], 0).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelNthBC";
        }
    };

    Modeler modelAmongBC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Among");
            IntVar[] vars = new IntVar[n - 1];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], vars[i]);
            }
            IntVar occVar = s.intVar("ovar", domains[n - 1][0], domains[n - 1][domains[n - 1].length - 1], false);
            if (map != null) map.put(domains[n - 1], occVar);
            int[] params = (int[]) parameters;
            s.among(occVar, vars, params).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelAmongBC";
        }
    };

    Modeler modelAmongAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Among");
            IntVar[] vars = new IntVar[n - 1];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            IntVar occVar = s.intVar("ovar", domains[n - 1]);
            if (map != null) map.put(domains[n - 1], occVar);
            int[] params = (int[]) parameters;
            s.among(occVar, vars, params).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelAmongAC";
        }
    };

    Modeler modelNValues = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("modelNValues_" + n);
            IntVar[] vars = new IntVar[n];
            IntVar[] decvars = new IntVar[n - 1];
            for (int i = 0; i < n; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
                if (i < n - 1) {
                    decvars[i] = vars[i];
                }
            }
            s.atLeastNValues(decvars, vars[n - 1], false).post();
            s.atMostNVvalues(decvars, vars[n - 1], false).post();
            for (String st : (String[]) parameters) {
                switch (st) {
                    case "at_least_AC":
                        new Constraint("atLeastNVAC", new PropAtLeastNValues_AC(decvars, vars[n - 1])).post();
                        break;
                    case "at_most_BC":
                        new Constraint("atMostBC", new PropAtMostNValues_BC(decvars, vars[n - 1])).post();
                        break;
                    case "at_most_greedy":
                        s.nValues(decvars, vars[n - 1]).post();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelNValues";
        }
    };

    Modeler modelGCC_alldiff = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("modelGCC_Fast_" + n);
            IntVar[] vars = new IntVar[n];
            TIntArrayList vals = new TIntArrayList();
            for (int i = 0; i < n; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                for (int j : domains[i]) {
                    if (!vals.contains(j)) {
                        vals.add(j);
                    }
                }
                if (map != null) map.put(domains[i], vars[i]);
            }
            int[] values = vals.toArray();
            IntVar[] cards = s.boolVarArray("cards", values.length);
            s.globalCardinality(vars, values, cards, false).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelGCC_alldiff";
        }
    };

    Modeler modelTree = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("tree_" + n);
            IntVar[] vars = new IntVar[n];
            IntVar[] succs = new IntVar[n - 1];
            for (int i = 0; i < n; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (i < n - 1) {
                    succs[i] = vars[i];
                }
                if (map != null) map.put(domains[i], vars[i]);
            }
            IntVar nbRoots = vars[n - 1];
            s.tree(succs, nbRoots, 0).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelTree";
        }
    };

    Modeler modelCircuit = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("circuit_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < n; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.circuit(vars, 0).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelCircuit";
        }
    };

    Modeler modelPath = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("path_" + n);
            IntVar[] vars = new IntVar[n - 2];
            for (int i = 0; i < n - 2; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            IntVar from = s.intVar("v_" + (n - 2), domains[n - 2]);
            if (map != null) map.put(domains[n - 2], from);
            IntVar to = s.intVar("v_" + (n - 1), domains[n - 1]);
            if (map != null) map.put(domains[n - 1], to);
            s.path(vars, from, to, 0).post();
            s.set(random_value(append(vars, new IntVar[]{from, to})));
            return s;
        }

        @Override
        public String name() {
            return "modelPath";
        }
    };

    Modeler modelSubcircuit = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("subcircuit_" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < n; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            s.subCircuit(vars, 0, s.intVar("length", 0, vars.length - 1, true)).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelSubcircuit";
        }
    };

    Modeler modelDiffn = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("diffn_" + n);
            IntVar[] vars = new IntVar[n];
            if (n % 4 != 0) {
                throw new UnsupportedOperationException();
            }
            int k = n / 4;
            IntVar[] x = new IntVar[k];
            IntVar[] y = new IntVar[k];
            IntVar[] dx = new IntVar[k];
            IntVar[] dy = new IntVar[k];
            for (int i = 0; i < n; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            for (int i = 0; i < k; i++) {
                x[i] = vars[i];
                y[i] = vars[i + k];
                dx[i] = vars[i + 2 * k];
                dy[i] = vars[i + 3 * k];
            }
            s.diffN(x, y, dx, dy, true).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelDiffn";
        }
    };

    Modeler modelCumulative = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver solver = new Solver("Cumulative_" + n);
            IntVar[] vars = new IntVar[n];
            if (n % 4 != 1) {
                throw new UnsupportedOperationException();
            }
            int k = n / 4;
            IntVar[] h = new IntVar[k];
            Task[] tasks = new Task[k];
            for (int i = 0; i < n; i++) {
                vars[i] = solver.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }
            for (int i = 0; i < k; i++) {
                tasks[i] = new Task(vars[i], vars[i + k], vars[i + 2 * k]);
                h[i] = vars[i + 3 * k];
            }
            IntVar capa = vars[vars.length - 1];
            solver.cumulative(tasks, h, capa, true).post();
            solver.set(random_bound(vars));
            return solver;
        }

        @Override
        public String name() {
            return "modelCumulative";
        }
    };

    Modeler modelSortBC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("Sort");

            IntVar[] X = new IntVar[n / 2];
            for (int i = 0; i < n / 2; i++) {
                X[i] = s.intVar("X_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], X[i]);
            }
            IntVar[] Y = new IntVar[n / 2];
            for (int i = n / 2; i < n; i++) {
                Y[i - n / 2] = s.intVar("Y_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], Y[i - n / 2]);
            }
            s.sort(X, Y).post();

            AbstractStrategy strategy = random_bound(append(X, Y), currentTimeMillis());
            s.set(strategy);
            return s;
        }

        @Override
        public String name() {
            return "modelSortBC";
        }
    };

    Modeler modelmddcAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("mddc_(sum)" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.mddc(vars, (MultivaluedDecisionDiagram) parameters).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelmddAC";
        }
    };

    Modeler modelivpcAC = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("ivpc" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("v_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.intValuePrecedeChain(vars, 1, 2).post();
            s.set(random_value(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelivpcAC";
        }
    };

    Modeler modelmaxbc = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("max" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("X_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.max(vars[0], copyOfRange(vars, 1, vars.length)).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelmaxbc";
        }
    };

    Modeler modelminbc = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("min" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("X_" + i, domains[i][0], domains[i][domains[i].length - 1], true);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.min(vars[0], copyOfRange(vars, 1, vars.length)).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelmaxbc";
        }
    };

    Modeler modelmaxbbc = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("maxb" + n);
            BoolVar[] vars = new BoolVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = domains[i].length > 1 ? s.boolVar("X_" + i) :
                        domains[i][0] == 0 ? s.boolVar(false) : s.boolVar(true);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.max(vars[0], copyOfRange(vars, 1, vars.length)).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelmaxbc";
        }
    };

    Modeler modelminbbc = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("minb" + n);
            BoolVar[] vars = new BoolVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = domains[i].length > 1 ? s.boolVar("X_" + i) :
                        domains[i][0] == 0 ? s.boolVar(false) : s.boolVar(true);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.min(vars[0], copyOfRange(vars, 1, vars.length)).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "modelminbc";
        }
    };

    Modeler modelplusbc = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("plus" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("X_" + i, domains[i][0], domains[i][domains[i].length - 1]);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.arithm(vars[0], "+", vars[1], "=", vars[2]).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "X+Y=Z (bc)";
        }
    };

    Modeler modelplusac = new Modeler() {
        @Override
        public Solver model(int n, int[][] domains, THashMap<int[], IntVar> map, Object parameters) {
            Solver s = new Solver("plus" + n);
            IntVar[] vars = new IntVar[n];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = s.intVar("X_" + i, domains[i]);
                if (map != null) map.put(domains[i], vars[i]);
            }

            s.arithm(vars[0], "+", vars[1], "=", vars[2]).post();
            s.set(random_bound(vars));
            return s;
        }

        @Override
        public String name() {
            return "X+Y=Z (ac)";
        }
    };
}
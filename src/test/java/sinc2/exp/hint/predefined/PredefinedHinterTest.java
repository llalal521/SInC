package sinc2.exp.hint.predefined;

import org.junit.jupiter.api.Test;
import sinc2.common.Argument;
import sinc2.common.ParsedArg;
import sinc2.common.ParsedPred;
import sinc2.common.Predicate;
import sinc2.exp.hint.ExperimentException;
import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;
import sinc2.kb.NumerationMap;
import sinc2.rule.BareRule;
import sinc2.rule.Fingerprint;
import sinc2.rule.Rule;
import sinc2.rule.RuleParseException;
import sinc2.util.MultiSet;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PredefinedHinterTest {
    static final String MEM_DIR = "D:\\sjtu\\KB_proj\\mem_cache";

    @Test
    void testRun1() throws KbException, IOException, ExperimentException, RuleParseException {
        // Test Dual Rule
        /* Rules:
         *   mother(X,Y):-child(Y,X)
         *   father(X,Y):-child(Y,X)
         */

        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";

        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int FAMILIES = 10;
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});

            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{daughter, dad});
            kb.addRecord(CHILD, new String[]{son, mom});
            kb.addRecord(CHILD, new String[]{daughter, mom});
        }
        kb.dump(MEM_DIR);

        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("Dual");
        hint_writer.close();

        PredefinedHinter pHinter=new PredefinedHinter();
        String[] main_args={MEM_DIR, KB_NAME, hint_file.getAbsolutePath()};
        pHinter.main(main_args);


        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("child(X,Y):-mother(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child(X,Y):-father(Y,X)", kb.getNumerationMap(), cache, tabu));

        String rules_root = pHinter.getOutputDirPath(hint_file.getAbsolutePath(), KB_NAME);
        File rules_file=Paths.get(rules_root,"rules_Dual.tsv").toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }

    @Test
    void testRun2() throws KbException, IOException, ExperimentException, RuleParseException {
        // Test Reflexive Rule
        /* Rules:
         *   self(X,X):-
         */

        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";

        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final String SELF = "self";
        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int FAMILIES = 10;
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});

            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{daughter, dad});
            kb.addRecord(CHILD, new String[]{son, mom});
            kb.addRecord(CHILD, new String[]{daughter, mom});

            kb.addRecord(SELF,new String[]{dad,dad});
            kb.addRecord(SELF,new String[]{mom,mom});
            kb.addRecord(SELF,new String[]{son,son});
            kb.addRecord(SELF,new String[]{daughter,daughter});
        }
        kb.dump(MEM_DIR);

        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("Reflexive");
        hint_writer.close();

        PredefinedHinter pHinter=new PredefinedHinter();
        String[] main_args={MEM_DIR, KB_NAME, hint_file.getAbsolutePath()};
        pHinter.main(main_args);


        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("self(X,X):-", kb.getNumerationMap(), cache, tabu));

        String rules_root = pHinter.getOutputDirPath(hint_file.getAbsolutePath(), KB_NAME);
        File rules_file=Paths.get(rules_root,"rules_Reflexive.tsv").toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }

    @Test
    void testRun3() throws KbException, IOException, ExperimentException, RuleParseException {
        // Test SubSumption Rule
        /* Rules:
         *   parent(X,Y):-mother(X,Y)
         *   parent(X,Y):-father(X,Y)
         */

        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";
        final String PARENT = "parent";
        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int FAMILIES = 10;
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});
            kb.addRecord(PARENT, new String[]{dad, son});
            kb.addRecord(PARENT, new String[]{mom, son});
            kb.addRecord(PARENT, new String[]{dad, daughter});
            kb.addRecord(PARENT, new String[]{mom, daughter});
            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{daughter, dad});
            kb.addRecord(CHILD, new String[]{son, mom});
            kb.addRecord(CHILD, new String[]{daughter, mom});
        }
        kb.dump(MEM_DIR);

        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("Subsumption");
        hint_writer.close();

        PredefinedHinter pHinter=new PredefinedHinter();
        String[] main_args={MEM_DIR, KB_NAME, hint_file.getAbsolutePath()};
        pHinter.main(main_args);


        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("parent(X,Y):-mother(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("parent(X,Y):-father(X,Y)", kb.getNumerationMap(), cache, tabu));

        String rules_root = pHinter.getOutputDirPath(hint_file.getAbsolutePath(), KB_NAME);
        File rules_file=Paths.get(rules_root,"rules_Subsumption.tsv").toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }

    @Test
    void testRun4() throws KbException, IOException, ExperimentException, RuleParseException {
        // Test Transition Rule
        /* Rules:
         *   father(X,Y):-couple(X,Z),mother(Z,Y)
         *   child(X,Y):-couple(Y,Z),mother(Z,X)
         *   mother(X,Y):-child(Y,Z),couple(Z,X)
         *   child(X,Y):-child(X,Z),couple(Z,Y) ??
         */

        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";
        final String COUPLE = "couple";
        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int FAMILIES = 10;
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});

            kb.addRecord(COUPLE, new String[]{dad, mom});

            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{daughter, dad});
            kb.addRecord(CHILD, new String[]{son, mom});
            kb.addRecord(CHILD, new String[]{daughter, mom});
        }
        kb.dump(MEM_DIR);

        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("Transition");
        hint_writer.close();

        PredefinedHinter pHinter=new PredefinedHinter();
        String[] main_args={MEM_DIR, KB_NAME, hint_file.getAbsolutePath()};
        pHinter.main(main_args);


        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("father(X,Y):-couple(X,Z),mother(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child(X,Y):-couple(Y,Z),mother(Z,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("mother(X,Y):-child(Y,Z),couple(Z,X)", kb.getNumerationMap(), cache, tabu));
        //expected_rules.add(parseBareRule("child(X,Y):-child(X,Z),couple(Z,Y)", kb.getNumerationMap(), cache, tabu));

        String rules_root = pHinter.getOutputDirPath(hint_file.getAbsolutePath(), KB_NAME);
        File rules_file=Paths.get(rules_root,"rules_Transition.tsv").toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }

    @Test
    void testRun5() throws KbException, IOException, ExperimentException, RuleParseException {
        // Test SharedSourceSink Rule
        /* Rules:
         *   child_f(X,Y):-child_m(X,Z),couple(Y,Z)
         *   father(X,Y):-couple(X,Z),child_m(Y,Z)
         *   child_m(X,Y):-father(Z,X),couple(Z,Y)
         *   mother(X,Y):-couple(Z,X),father(Z,Y)
         *   couple(X,Y):-father(X,Z),mother(Y,Z) --
         *   couple(X,Y):-child_f(Z,X),child_m(Z,Y) --
         */

        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";
        final String COUPLE = "couple";
        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD_F = "child_f";
        final String CHILD_M = "child_m";
        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int FAMILIES = 10;
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});

            kb.addRecord(COUPLE, new String[]{dad, mom});

            kb.addRecord(CHILD_F, new String[]{son, dad});
            kb.addRecord(CHILD_F, new String[]{daughter, dad});
            kb.addRecord(CHILD_M, new String[]{son, mom});
            kb.addRecord(CHILD_M, new String[]{daughter, mom});
        }
        kb.dump(MEM_DIR);

        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("SharedSourceSink");
        hint_writer.close();

        PredefinedHinter pHinter=new PredefinedHinter();
        String[] main_args={MEM_DIR, KB_NAME, hint_file.getAbsolutePath()};
        pHinter.main(main_args);


        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("child_f(X,Y):-child_m(X,Z),couple(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("father(X,Y):-couple(X,Z),child_m(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child_m(X,Y):-father(Z,X),couple(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("mother(X,Y):-couple(Z,X),father(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("couple(X,Y):-father(X,Z),mother(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("couple(X,Y):-child_f(Z,X),child_m(Z,Y)", kb.getNumerationMap(), cache, tabu));

        String rules_root = pHinter.getOutputDirPath(hint_file.getAbsolutePath(), KB_NAME);
        File rules_file=Paths.get(rules_root,"rules_SharedSourceSink.tsv").toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }
    BareRule parseBareRule(String str, NumerationMap numMap, Set<Fingerprint> cache, Map<MultiSet<Integer>, Set<Fingerprint>> tabu) throws RuleParseException {
        List<ParsedPred> parsed_structure = Rule.parseStructure(str);
        List<Predicate> structure = new ArrayList<>();
        for (ParsedPred parsed_pred: parsed_structure) {
            Predicate predicate = new Predicate(numMap.name2Num(parsed_pred.functor), parsed_pred.args.length);
            for (int arg_idx = 0; arg_idx < parsed_pred.args.length; arg_idx++) {
                ParsedArg parsed_arg = parsed_pred.args[arg_idx];
                if (null == parsed_arg) {
                    predicate.args[arg_idx] = Argument.EMPTY_VALUE;
                } else if (null == parsed_arg.name) {
                    predicate.args[arg_idx] = Argument.variable(parsed_arg.id);
                } else {
                    predicate.args[arg_idx] = Argument.constant(numMap.name2Num(parsed_arg.name));
                }
            }
            structure.add(predicate);
        }
        return new BareRule(structure, cache, tabu);
    }
    @Test
    void testRun6() throws KbException, IOException, ExperimentException, RuleParseException {
        // Test TypeInference Rule
        /* Rules:
         *   kid(X):-child(X,Y)
         *   kid(X):-father(Y,X)
         *   kid(X):-mother(Y,X)
         *   kid(X):-family(Y,Z,X)
         */

        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";

        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final String KID = "kid";

        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int FAMILIES = 10;

        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});

            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{daughter, dad});
            kb.addRecord(CHILD, new String[]{son, mom});
            kb.addRecord(CHILD, new String[]{daughter, mom});

            kb.addRecord(KID, new String[]{daughter});
            kb.addRecord(KID, new String[]{son});
        }
        kb.dump(MEM_DIR);

        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("TypeInference");
        hint_writer.close();

        PredefinedHinter pHinter=new PredefinedHinter();
        String[] main_args={MEM_DIR, KB_NAME, hint_file.getAbsolutePath()};
        pHinter.main(main_args);


        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("kid(X):-child(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("kid(X):-father(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("kid(X):-mother(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("kid(X):-family(Y,Z,X)", kb.getNumerationMap(), cache, tabu));

        String rules_root = pHinter.getOutputDirPath(hint_file.getAbsolutePath(), KB_NAME);
        File rules_file=Paths.get(rules_root,"rules_TypeInference.tsv").toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }
    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

}
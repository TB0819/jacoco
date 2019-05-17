package org.jacoco.report;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jacoco.core.internal.diff.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JGitTest {
    static Git git;
    static Repository repository;
    private String diffLog = "C:\\workspace\\diffLog.txt";
    private String diffLog1 = "C:\\workspace\\diffLog1.txt";
    private String diffLog2 = "C:\\workspace\\diffLog2.txt";

    @BeforeClass
    public static void BeforeClass() {
        try {
            git = Git.open(new File("C:\\workspace\\companywork\\demo"));
            repository = git.getRepository();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public  void test_changeBranchTest() throws GitAPIException {
        GitAdapter gitAdapter = new GitAdapter("C:\\workspace\\companywork\\pressure-agent");
        gitAdapter.checkOutAndPull(null, "pressure");
    }

    @Test
    public void test_19() throws GitAPIException {
//        GitAdapter gitAdapter = new GitAdapter("C:\\workspace\\companywork\\demo");
//        gitAdapter.checkOut( "jacoco-cov");
        List<ClassInfo> list = CodeDiff.diffBranchToMaster("C:\\workspace\\companywork\\demo","jacoco-cov");
        System.err.println(list.size());
    }

    @Test
    public void test_12() throws Exception {
//        List<MethodInfo> list = CodeDiff.diffBranchToMaster("C:\\workspace\\companywork\\supplychain-soa","open_interface");
        List<ClassInfo> list = CodeDiff.diffBranchToBranch("C:\\workspace\\companywork\\supplychain-soa","open_interface","master");
       int count = 0;
        for (ClassInfo classInfo: list) {
            count = count + classInfo.getMethodInfos().size();
        }
        System.err.println(count);
    }
    @Test
    public void test_3() throws GitAPIException {
        //  授权
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("cangzhu","TB#19910819");

        Collection<Ref> refs = git.lsRemote().setCredentialsProvider(credentialsProvider).call();
//        for (Ref ref : refs) {
//            System.out.println("Ref: " + ref);
//        }
        // heads only
        refs = git.lsRemote().setCredentialsProvider(credentialsProvider).setHeads(true).call();
        for (Ref ref : refs) {
            System.out.println("Head: " + ref);
        }
//
//        // tags only
//        refs = git.lsRemote().setCredentialsProvider(credentialsProvider).setTags(true).call();
//        for (Ref ref : refs) {
//            System.out.println("Remote tag: " + ref);
//        }
    }


    @Test
    public void test_5() throws GitAPIException {
        GitAdapter gitAdapter = new GitAdapter("C:\\workspace\\companywork\\supplychain-soa");
        gitAdapter.checkOut("open_interface");
//        gitAdapter.checkOut("master");
    }

    @Test
    public void test_0() throws GitAPIException, IOException {
        List<Ref> branchList =  git.branchList().call();
        List<ObjectId> versionList = new ArrayList<ObjectId>();
        for (Ref ref : branchList) {
            System.err.println(ref.getObjectId() + " --- " + ref.getName());
            versionList.add(ref.getObjectId());
        }
        AbstractTreeIterator newTree = versionParser(versionList.get(0));
        AbstractTreeIterator oldTree = versionParser(versionList.get(1));
        List<DiffEntry> diffs = git.diff().setOldTree(oldTree).setNewTree(newTree).setShowNameAndStatusOnly(true).call();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter df = new DiffFormatter(out);
        //设置比较器为忽略空白字符对比（Ignores all whitespace）
        df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
        df.setRepository(git.getRepository());
        FileWriter fw = new FileWriter(diffLog);
        BufferedWriter bufferedWriter = new BufferedWriter(fw);
        for (DiffEntry diffEntry : diffs) {
            bufferedWriter.write("------------------------------start-----------------------------\n");
//            System.out.println("------------------------------start-----------------------------");
            //打印文件差异具体内容
            df.format(diffEntry);
            String diffText = out.toString("UTF-8");
//            System.out.println(diffText);
            bufferedWriter.write(diffText);

            //获取文件差异位置，从而统计差异的行数，如增加行数，减少行数
            FileHeader fileHeader = df.toFileHeader(diffEntry);
            int addSize = 0;
            int subSize = 0;
            EditList editList = fileHeader.toEditList();
            for(Edit edit : editList){
                String eidtstr = edit.toString();
                int alength = edit.getLengthA();
                int blength = edit.getLengthB();
                String type = edit.getType().toString();
                subSize += edit.getEndA()-edit.getBeginA();
                addSize += edit.getEndB()-edit.getBeginB();
            }
            bufferedWriter.write("------------------------------addSize=  "+addSize+"-----------------------------\n");
            bufferedWriter.write("------------------------------subSize=  "+subSize+"-----------------------------\n");
            bufferedWriter.write("------------------------------end-----------------------------\n");
//            System.out.println("------------------------------end-----------------------------");
            out.reset();
        }
        bufferedWriter.close();
    }

    @Test
    public void test_2() throws IOException, GitAPIException {
        AbstractTreeIterator newTreeParser = versionParser( "refs/heads/jacoco-cov");
        AbstractTreeIterator oldTreeParser = versionParser("refs/heads/master");
        List<DiffEntry> diffs = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).setShowNameAndStatusOnly(true).call();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter df = new DiffFormatter(out);
        //设置比较器为忽略空白字符对比（Ignores all whitespace）
        df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
        df.setRepository(git.getRepository());
        FileWriter fw = new FileWriter(diffLog1);
        BufferedWriter bufferedWriter = new BufferedWriter(fw);
        for (DiffEntry diffEntry : diffs) {
            //  排除测试类
            if (diffEntry.getNewPath().contains("/src/test/java/")) {
                continue;
            }
            //  非java文件 和 删除内容不记录
            if (!diffEntry.getNewPath().endsWith(".java") || diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE){
                continue;
            }
            //打印文件差异具体内容
            bufferedWriter.write("------------------------------start   "+diffEntry.getChangeType().toString()+" -----------------------------\n");
            df.format(diffEntry);
            String diffText = out.toString("UTF-8");
//              System.out.println(diffText);
            bufferedWriter.write(diffText);
            bufferedWriter.write("------------------------------end-----------------------------\n");
            out.reset();
//                ASTGenerator astGenerator = new ASTGenerator("C:\\workspace\\companywork\\supplychain-soa\\" + diffEntry.getNewPath());
            if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD) {
                ASTGenerator astGenerator = new ASTGenerator(diffText);
                List<MethodInfo> list = astGenerator.getMethodInfoList();
                bufferedWriter.write("------------------------------end  "+diffEntry.getChangeType().toString()+"   方法个数： "+list.size()+"-----------------------------\n");
            }
            String newJavaPath = diffEntry.getNewPath();
            String oldJavaPath = diffEntry.getOldPath();
            //获取文件差异位置，从而统计差异的行数，如增加行数，减少行数
            FileHeader fileHeader = df.toFileHeader(diffEntry);
            int addSize = 0;
            int subSize = 0;
            EditList editList = fileHeader.toEditList();
            for(Edit edit : editList){
                String eidtstr = edit.toString();
                int alength = edit.getLengthA();
                int blength = edit.getLengthB();
                String type = edit.getType().toString();
                subSize += edit.getEndA()-edit.getBeginA();
                addSize += edit.getEndB()-edit.getBeginB();
            }
            String[] strArr = diffEntry.getNewPath().split("/");
            String className = strArr[strArr.length - 1].replaceAll(".java","");
        }
        bufferedWriter.close();
    }

    @Test
    public void test_1() throws GitAPIException, IOException {
        RevWalk walk = new RevWalk(repository);
        List<RevCommit> commitList = new ArrayList<RevCommit>();
        Iterable<RevCommit> commits = git.log().setMaxCount(2).call();
        //  获取最近提交的两次记录
        for(RevCommit commit:commits){
            commitList.add(commit);
            System.out.println(commit.getFullMessage());
            System.out.println(commit.getAuthorIdent().getWhen());
        }
        if(commitList.size()==2){
            AbstractTreeIterator newTree = prepareTreeParser(commitList.get(0));
            AbstractTreeIterator oldTree = prepareTreeParser(commitList.get(1));
            List<DiffEntry> diff = git.diff().setOldTree(oldTree).setNewTree(newTree).setShowNameAndStatusOnly(true).call();


            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter(out);
            //设置比较器为忽略空白字符对比（Ignores all whitespace）
            df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            df.setRepository(git.getRepository());
            System.out.println("------------------------------start-----------------------------");
            //每一个diffEntry都是第个文件版本之间的变动差异
            for (DiffEntry diffEntry : diff) {
                //打印文件差异具体内容
                df.format(diffEntry);
                String diffText = out.toString("UTF-8");
                System.out.println(diffText);

                //获取文件差异位置，从而统计差异的行数，如增加行数，减少行数
                FileHeader fileHeader = df.toFileHeader(diffEntry);
                int addSize = 0;
                int subSize = 0;
                EditList editList = fileHeader.toEditList();
                for(Edit edit : editList){
                    subSize += edit.getEndA()-edit.getBeginA();
                    addSize += edit.getEndB()-edit.getBeginB();

                }
                System.out.println("addSize="+addSize);
                System.out.println("subSize="+subSize);
                System.out.println("------------------------------end-----------------------------");
                out.reset();
            }
        }
    }

    @Test
    public void test_8() throws IOException {
        Ref branch = repository.exactRef("refs/heads/open_interface");
        ObjectId objId = branch.getObjectId();
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(objId);
        TreeWalk treeWalk = TreeWalk.forPath(repository, "supplychain-client/src/main/java/com/dfire/soa/supplychain/query/SupplierQuery.java", tree);
        ObjectId blobId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(blobId);
        byte[] bytes = loader.getBytes();
        System.err.println(new String(bytes));
        walk.dispose();
    }

    @Test
    public void test_9 () throws IOException {
        Ref branch = repository.exactRef("refs/heads/master");
        ObjectId objId = branch.getObjectId();
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(objId);
        TreeWalk treeWalk = TreeWalk.forPath(repository, "supplychain-client/src/main/java/com/dfire/soa/supplychain/query/SupplierQuery.java", tree);
        ObjectId blobId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(blobId);
        byte[] bytes = loader.getBytes();
        System.err.println(new String(bytes));
        walk.dispose();
    }

    @Test
    public void test_11(){
        String baseDir = System.getProperty("baseDir");
        System.err.println(baseDir);
    }

    public AbstractTreeIterator prepareTreeParser(RevCommit commit) {
        System.out.println(commit.getId());
        try {
            RevWalk walk = new RevWalk(repository);
            System.out.println(commit.getTree().getId());
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            ObjectReader oldReader = repository.newObjectReader();
            oldTreeParser.reset(oldReader, tree.getId());

            walk.dispose();

            return oldTreeParser;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public AbstractTreeIterator versionParser(ObjectId objectId) throws IOException {
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(objectId);
        ObjectReader objectReader = repository.newObjectReader();
        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        oldTreeParser.reset(objectReader, tree.getId());
        walk.dispose();
        return oldTreeParser;
    }

    public AbstractTreeIterator versionParser(String ref) throws IOException {
        Ref head = repository.exactRef(ref);
        RevWalk walk = new RevWalk(repository);
        System.out.println(ref+ "  --  " + head + " --name:- " +head.getName() + "  -id:-  "+ head.getObjectId().getName());
        RevCommit commit = walk.parseCommit(head.getObjectId());
        RevTree tree = walk.parseTree(commit.getTree().getId());
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        treeParser.reset(reader, tree.getId());
        walk.dispose();
        return treeParser;
    }
}

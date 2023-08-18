package com.test.gitclient;

import com.test.gitclient.auth.GitAuthStrategy;
import com.test.gitclient.auth.HttpAuthStrategy;
import com.test.gitclient.auth.SshAuthStrategy;
import com.test.gitclient.format.ContentFormatter;
import com.test.gitclient.format.FormatEntry;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.*;
import java.util.stream.Collectors;

public class GitClient {

    final static Logger logger = LoggerFactory.getLogger(GitClient.class);

    private final GitAuthStrategy authStrategy;

    public GitClient(String privateKeyPath) {
        this.authStrategy = new SshAuthStrategy(privateKeyPath);
    }

    public GitClient(String userName, String password) {
        this.authStrategy = new HttpAuthStrategy(userName, password);
    }


    public boolean clone(String remoteUri, String localPath) {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(remoteUri)
                .setDirectory(new File(localPath));
        try {
            this.authStrategy.auth(cloneCommand).call();
            return true;
        }catch (GitAPIException e){
            logger.error("克隆代码报错异常: {}", e.getMessage());
            return false;
        }
    }

    public static List<String> getLocalBranchList(String localPath) {

        try {
            return Git.open(new File(localPath)).branchList().call()
                    .stream().map(ref-> Repository.shortenRefName(ref.getName()))
                    .collect(Collectors.toList());
        }catch (IOException | GitAPIException e){
            logger.error("获取本地分支异常: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public static List<String> getRemoteBranchList(String localPath) {
        try {
            return Git.open(new File(localPath))
                    .branchList().setListMode(ListBranchCommand.ListMode.REMOTE)

                    .call()
                    .stream().map(ref-> Repository.shortenRefName(ref.getName()))
                    .collect(Collectors.toList());
        }catch (IOException | GitAPIException e){
            logger.error("获取远程分支异常: {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    public static boolean newBranch(String localPath, String branchName){
        try {
            Git.open(new File(localPath)).branchCreate()
                    .setName(branchName).call();
            return true;
        }catch (IOException | GitAPIException e){
            logger.error("创建分支异常: {}", e.getMessage());
        }
        return false;
    }

    public static boolean deleteBranch(String localPath, String branchName){
        try {
            Git.open(new File(localPath)).branchDelete().setBranchNames(branchName).call();
            return true;
        }catch (IOException | GitAPIException e){
            logger.error("删除分支异常: {}", e.getMessage());
        }
        return false;
    }

    public static boolean newBranch(String localPath, String branchName, String trackingBranchName){
        try {
            Git.open(new File(localPath)).branchCreate()
                    .setName(branchName).setStartPoint(trackingBranchName).call();
            return true;
        }catch (IOException | GitAPIException e){
            logger.error("创建远程分支异常: {}", e.getMessage());
        }
        return false;
    }

    public static List<String> checkout(String localPath,String branchName) {

        try {
            Git.open(new File(localPath)).checkout().setName(branchName).call();
        } catch (CheckoutConflictException e){
            return e.getConflictingPaths();
        } catch (IOException | GitAPIException e) {
            logger.error("切换分支异常: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public static boolean add(String localPath, String[] fileNames) {

        try {
            AddCommand add = Git.open(new File(localPath)).add();
            Arrays.stream(fileNames).forEach(add::addFilepattern);
            add.call();
            return true;
        } catch (IOException | GitAPIException e) {
            logger.error("添加失败: {}", e.getMessage());
        }
        return false;
    }

    public static String getTrackingBranch(String localPath){
        try {
            Repository repository = Git.open(new File(localPath)).getRepository();
            String branch = repository.getBranch();
            BranchConfig branchConfig = new BranchConfig(repository.getConfig(), branch);
            return Repository.shortenRefName(branchConfig.getTrackingBranch());
        } catch (IOException e) {
            logger.error("获取追踪的分支失败: {}", e.getMessage());
        }
        return null;
    }

    public static boolean commit(String localPath, String desc) {
        try {
            Git.open(new File(localPath)).commit().setMessage(desc).call();
            return true;
        } catch (IOException | GitAPIException e) {
            logger.error("提交失败: {}", e.getMessage());
        }
        return false;
    }

    public static String getRemoteUrl(String localPath) {
        String remoteUrl = null;
        try {
            StoredConfig config = Git.open(new File(localPath)).getRepository().getConfig();
            Set<String> remote = config.getSubsections("remote");
            for (String remoteName: remote) {
                remoteUrl = config.getString("remote", remoteName, "url");
            }
        } catch (IOException e) {
            logger.error("获取远程地址异常: {}", e.getMessage());
        }
        return remoteUrl;
    }

    public boolean push(String localGit) {

        try {
            PushCommand push = Git.open(new File(localGit)).push();
            this.authStrategy.auth(push).call();
            return true;
        } catch (IOException | GitAPIException e) {
            logger.error("推送失败: {}", e.getMessage());
        }
        return false;
    }

    public List<String> pull(String localGit) {

        try {
            PullCommand pull = Git.open(new File(localGit)).pull();
            this.authStrategy.auth(pull).call();
        } catch (CheckoutConflictException e){
            return e.getConflictingPaths();
        } catch (IOException | GitAPIException e) {
            logger.error("同步失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     *
     * @param localGit
     * @param branchName 要合并的分支名，支持本地分支与远程分支
     * @param conflictFiles 冲突文件列表
     * @return 合并结果：FAILED：说明有未提交文件；CONFLICTING：有冲突且自动合并失败，通过conflictFiles获取冲突文件，其他状态默认已合并成功。
     */
    public static MergeResult.MergeStatus merge(String localGit, String branchName, List<String> conflictFiles) {

        try {
            Git git = Git.open(new File(localGit));
            Repository repository = git.getRepository();
            MergeResult merge = git.merge().include(repository.resolve(branchName))
                    .setCommit(true)
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                    .setMessage("Merged " + branchName)
                    .call();
            MergeResult.MergeStatus mergeStatus = merge.getMergeStatus();

            if ("CONFLICTING".equals(mergeStatus.name())) {
                Set<Map.Entry<String, int[][]>> entries = merge.getConflicts().entrySet();
                for (Map.Entry<String, int[][]> entry : entries) {
                    conflictFiles.add(entry.getKey());
                }
            }
            return merge.getMergeStatus();
        }catch (IOException | GitAPIException e) {
            logger.error("同步失败: {}", e.getMessage());
        }
        return MergeResult.MergeStatus.FAILED;
    }


    public static Iterable<RevCommit> logs(String localGit) {

        try (Git git = Git.open(new File(localGit))) {
            Iterable<RevCommit> commits = git.log().call();
            return commits;
        }catch (Exception e) {
            logger.error("gitLogs error! \n" + e.getMessage());
        }
        return null;
    }


    public static boolean reset(String localGit,String commitName,String resetType) {

        boolean resetFlag = true;
        ResetCommand.ResetType mode = null;
        try (Git git = Git.open(new File(localGit))) {
            RevWalk walk = new RevWalk(git.getRepository());
            ObjectId objectId = git.getRepository().resolve(commitName);
            RevCommit revCommit = walk.parseCommit(objectId);
            String perVision = revCommit.getName();
            if(resetType.equals("hard")){
                mode = ResetCommand.ResetType.HARD;
            }
            if(resetType.equals("mixed")){
                mode = ResetCommand.ResetType.MIXED;
            }
            if(resetType.equals("soft")){
                mode = ResetCommand.ResetType.SOFT;
            }
            git.reset().setMode(mode).setRef(perVision).call();
        } catch (IOException | GitAPIException e) {
            resetFlag = false;
            logger.error("Reset error! \n" + e.getMessage());
        }
        return resetFlag;
    }

    public static List<FormatEntry> localDiff(String localGit) {
        List<FormatEntry> ret = new ArrayList<>();

        try (Git git = Git.open(new File(localGit))) {
            Repository repository = git.getRepository();

            OutputStream outputStream = new ByteArrayOutputStream();

            DiffFormatter diffFormatter = new DiffFormatter(outputStream);
            diffFormatter.setRepository(repository);

            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            ObjectId headTree = repository.resolve("HEAD^{tree}");
            if (headTree == null) return ret;
            oldTree.reset(reader, headTree);
            FileTreeIterator newTree = new FileTreeIterator(repository);
            diffFormatter.format(oldTree, newTree);
            ret = ContentFormatter.parserEntry(outputStream.toString());
            return ret;
        } catch (IOException e) {
            logger.error("显示差异异常: ", e);
        }
        return ret;
    }

    public static List<FormatEntry> branchDiff(String localGit, String oldBranch, String newBranch) {

        try (Git git = Git.open(new File(localGit))) {
            Repository repository = git.getRepository();
            Ref oldRef = repository.findRef(oldBranch);
            Ref newRef = repository.findRef(newBranch);

            AbstractTreeIterator oldTree = prepareTreeParser(repository, oldRef);
            AbstractTreeIterator newTree = prepareTreeParser(repository, newRef);

            return showDiff(git, oldTree, newTree);
        } catch (IOException | GitAPIException e) {
            logger.error("显示差异异常: ", e);
        }
        return null;
    }

    public static List<FormatEntry> commitDiff(String localGit, String oldCommit, String newCommit) {

        try (Git git = Git.open(new File(localGit))) {
            Repository repository = git.getRepository();

            AbstractTreeIterator oldTree = prepareTreeParser(repository, oldCommit);
            AbstractTreeIterator newTree = prepareTreeParser(repository, newCommit);
            return showDiff(git, oldTree, newTree);
        } catch (IOException | GitAPIException e) {
            logger.error("显示差异异常: ", e);
        }
        return null;
    }

    public static List<FormatEntry> showDiff(Git git, AbstractTreeIterator oldTree, AbstractTreeIterator newTree) throws GitAPIException {
        List<FormatEntry> ret = new ArrayList<>();
        List<DiffEntry> diff = git.diff().setOldTree(oldTree).setNewTree(newTree).call();
        diff.stream().forEach(entity -> {
            OutputStream outputStream = new ByteArrayOutputStream();
            DiffFormatter diffFormatter = new DiffFormatter(outputStream);
            diffFormatter.setRepository(git.getRepository());
            try {
                diffFormatter.format(entity);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ret.addAll(ContentFormatter.parserEntry(outputStream.toString()));
        });
        return ret;
    }


    private static AbstractTreeIterator prepareTreeParser(Repository repository, Ref ref) throws IOException {
        return prepareTreeParser(repository, ref.getObjectId());
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        return prepareTreeParser(repository, ObjectId.fromString(objectId));
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }


    public static Map<String,List<String>> gitStatus(String localGit) {
        Map<String,List<String>> map = new HashMap<>();
        try (Git git = Git.open(new File(localGit))) {
            Status status = git.status().call();
            map.put("Added", setToList(status.getAdded()));
            map.put("Changed",setToList(status.getChanged()));
            map.put("Conflicting",setToList(status.getConflicting()));
            map.put("Missing",setToList(status.getMissing()));
            map.put("IgnoredNotInIndex",setToList(status.getIgnoredNotInIndex()));
            map.put("Modified",setToList(status.getModified()));
            map.put("Removed",setToList(status.getRemoved()));
            map.put("Untracked",setToList(status.getUntracked()));
            map.put("UntrackedFolders",setToList(status.getUntrackedFolders()));
            map.put("UncommittedChanges",setToList(status.getUncommittedChanges()));

        } catch (Exception e) {
            logger.error("获取git状态异常{}", e.getMessage());
        }
        return map;
    }


    public static List<String> gitBranchList(String localGit) {
        List<String> barchList = new ArrayList<>();
        try (Git git = Git.open(new File(localGit))) {
            List<Ref> refs = git.branchList().call();
            if (refs.size() > 0) {
                for (Ref ref : refs) {
                    barchList.add(ref.getName().replace("refs/heads/",""));
                }
            }
        } catch (Exception e) {
            logger.error("获取git分支异常{}", e.getMessage());
        }
        return barchList;
    }

    public static List<String> setToList(Set<String> set) {
        List<String> list = new ArrayList<>();
        for(String s: set) {
            list.add(s);
        }
        return list;
    }


}

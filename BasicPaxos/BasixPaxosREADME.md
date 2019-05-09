# BasicPaxos

实现了朴素的Paxos算法。

**协议过程(来源微信团队)**

**第一阶段A**

Proposer选择一个提议编号n，向所有的Acceptor广播Prepare（n）请求。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508145721.png)

**第一阶段B**

Acceptor接收到Prepare（n）请求，若提议编号n比之前接收的Prepare请求都要大，则承诺将不会接收提议编号比n小的提议，并且带上之前**Accept的提议（必须是Accept之后的，如果只是prepare阶段的提议，那么Acceptor会从中选择一个最大的）**中编号小于n的最大的提议，否则不予理会。

**注解**：我的理解是「带上之前Accept的提议中编号小于n的最大的提议」，这个只是提议，并没有被最终确定（就是说没有经历第二阶段B，没有被最终写入），本质上属于投票阶段。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508145856.png)

**第二阶段A**

Proposer得到了多数Acceptor的承诺后，如果没有发现有一个Acceptor接受过一个值，那么向所有的Acceptor发起自己的值和提议编号n，否则，从所有接受过的值中选择对应的提议编号最大的，作为提议的值，提议编号仍然为n。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508150005.png)

**第二阶段B**

Acceptor接收到提议后，如果该提议编号不违反自己做过的承诺，则接受该提议。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508150027.png)

需要注意的是，Proposer发出Prepare（n）请求后，得到多数派的应答，然后可以随便再选择一个多数派广播Accept请求，而**不一定要将Accept请求发给有应答的Acceptor**.



在该算法的实现中，只考虑了一个节点进行提案的情况，Acceptor节点随机性的进行故障来进行模拟的。同时没有考虑到Learner。

# 代码组织架构

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508150326.png)

其中BasicPaxos为项目的主类。

# 核心算法

第一阶段Prepare：

```java
/**
 * Proposer选择一个提案编号n，发送Prepare(n)请求给超过半数（或更多）的Acceptor。
 * Acceptor收到消息后，如果n比它之前见过的编号大，就回复这个消息，而且以后不会接受小于n的提案。另外，如果之前已经接受了小于n的提案，回复那个提案编号和内容给Proposer。
 * @param proposal 提案
 * @return Promise 承诺
 */
public Promise onPrepare(Proposal proposal) {
    //假设这个过程有50%的几率失败，用随机数来模拟节点出现故障的情况
    if (Math.random() - 0.5 > 0) {
        printInfo("ACCEPTER_" + name, "PREPARE", "NO RESPONSE");
        return null;
    }
    //判断非法输入
    if (proposal == null)
        throw new IllegalArgumentException("null proposal");
    //如果提案编号n比它之前见过的编号大，就回复这个消息，而且以后不会接受小于n的提案；另外，如果之前已经接受了小于n的提案，回复那个提案编号和内容给Proposer。
    if (proposal.getVoteNumber() > last.getVoteNumber()) {
        Promise response = new Promise(true, last);
        last = proposal;
        printInfo("ACCEPTER_" + name, "PREPARE", "OK");
        return response;
    } else {//当前提案的编号过小
        printInfo("ACCEPTER_" + name, "PREPARE", "REJECTED");
        return new Promise(false, null);
    }
}
```

第二阶段Accept

```java
/**
 * Acceptor收到消息后，如果n大于等于之前见过的最大编号，就记录这个提案编号和内容，回复请求表示接受
 * @param proposal
 * @return
 */
public boolean onAccept(Proposal proposal) {
    //假设这个过程有50%的几率失败
    if (Math.random() - 0.5 > 0) {
        printInfo("ACCEPTER_" + name, "ACCEPT", "NO RESPONSE");
        return false;
    }
    printInfo("ACCEPTER_" + name, "ACCEPT", "OK");
    return last.equals(proposal);
}
```

当这轮投票失败后，会发起下一轮投票：

```java
/**
 * 对于提案的约束，第三条约束要求：
 * 如果maxVote不存在，那么没有限制，下一次表决可以使用任意提案；
 * 否则，下一次表决要沿用maxVote的提案
 *
 * @param currentVoteNumber
 * @param proposals
 * @return
 */
public static Proposal nextProposal(long currentVoteNumber, List<Proposal> proposals) {
    long voteNumber = currentVoteNumber + 1;
    //如果当前没有提案，则随机生成一个提案
    if (proposals.isEmpty())
        return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
    //先排序，然后找最大提案
    Collections.sort(proposals);
    Proposal maxVote = proposals.get(proposals.size() - 1);
    long maxVoteNumber = maxVote.getVoteNumber();
    String content = maxVote.getContent();
    if (maxVoteNumber >= currentVoteNumber)
        throw new IllegalStateException("illegal state maxVoteNumber");
    if (content != null)
        return new Proposal(voteNumber, content);
    else 
      return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
}
```



# 运行结果

运行方法：

把项目编译以后运行BasicPaxos的main方法即可。

某次运行结果及如下图所示：

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508182005.png)

注释：第一轮结束后，在ACCEPT阶段，由于没有获取到大多数节点的回复，导致失败。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508182100.png)

注释：第二轮结束后，在ACCEPT阶段，由于没有获取到大多数节点的回复，导致失败。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508182137.png)

注释：第三轮结束后，在Prepare阶段，由于没有获取到大多数节点的回复，导致失败。![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190508182219.png)

第四轮结束后，prepare阶段，获取3个节点，即半数节点的回应，在Accept阶段，获取到半数以上节点的回应，提案成功。

当然每次运行都可能会有不同的结果。

# 参考资料

[微信团队phxPaxos](https://github.com/Tencent/phxpaxos/wiki)
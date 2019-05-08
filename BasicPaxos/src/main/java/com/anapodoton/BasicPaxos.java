package com.anapodoton;

import com.anapodoton.role.Acceptor;
import com.anapodoton.role.Proposer;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BasicPaxos
 * Proposer:提案者
 * Acceptor：接受者
 * Proposal：提案
 * Promise：承诺
 */
public  class BasicPaxos {
    public static final HashFunction HASH_FUNCTION = Hashing.murmur3_32();
    public static final Random RANDOM = new Random();
    public static final String[] PROPOSALS = {"ProjectA", "ProjectB", "ProjectC"};

    public static void main(String[] args) {
        //5个Acceptor
        List<Acceptor> acceptors = new ArrayList<Acceptor>();
        acceptors.add(new Acceptor("A"));
        acceptors.add(new Acceptor("B"));
        acceptors.add(new Acceptor("C"));
        acceptors.add(new Acceptor("D"));
        acceptors.add(new Acceptor("E"));
        //Proposer向所有Acceptor发起投票
        Proposer.vote(new Proposal(1L, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]), acceptors);
    }

    public static void printInfo(String subject, String operation, String result) {
        System.out.println(subject + ":" + operation + "<" + result + ">");
    }

}

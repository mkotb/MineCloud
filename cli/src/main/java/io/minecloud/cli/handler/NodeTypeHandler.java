/*
 * Copyright (c) 2015, Mazen Kotb <email@mazenmc.io>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package io.minecloud.cli.handler;

import asg.cliche.Command;
import asg.cliche.Param;
import io.minecloud.MineCloud;
import io.minecloud.models.nodes.type.CPU;
import io.minecloud.models.nodes.type.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NodeTypeHandler extends AbstractHandler {
    NodeType type;

    NodeTypeHandler(String name) {
        type = MineCloud.instance().mongo()
                .repositoryBy(NodeType.class)
                .findFirst(name);

        if (type == null) {
            System.out.println("Could not find type in database; creating new one...");
            type = new NodeType();

            type.setName(name);
        }
    }

    @Command
    public String ram(@Param(name = "amount") int amount) {
        if (amount < 0) {
            return "Invalid amount!";
        }

        type.setRam(amount);
        return "Set ram on node type " + type.name() + " to " +
                amount + " MB";
    }

    @Command
    public String cpu() {
        System.out.println("To continue, answer the following questions about the CPU for this node type");
        System.out.println("\n");

        Scanner scanner = new Scanner(System.in);
        CPU cpu = new CPU();

        System.out.print("What is the base frequency on this processor? ");
        cpu.setBaseFrequency(scanner.nextDouble());

        System.out.print("What is the max frequency on this processor? ");
        cpu.setMaxFrequency(scanner.nextDouble());

        System.out.print("How many cores are on this processor? ");
        cpu.setCores(scanner.nextInt());

        System.out.print("How many threads are on this processor? ");
        cpu.setThreads(scanner.nextInt());

        System.out.println("Thank you!");

        type.setCpu(cpu);
        return "Successfully set CPU to entered values";
    }

    @Command
    public String push() {
        if (type.processor() == null || type.ram() == 0) {
            return "Required fields (cpu, ram) have not been set by the user! Unable to push modifications";
        }

        MineCloud.instance().mongo()
                .repositoryBy(NodeType.class)
                .save(type);
        return "Successfully pushed modifications to database!";
    }

    @Command(name = "!show")
    public List<String> show() {
        List<String> list = new ArrayList<>();
        list.add("Currently Modeling [Node Type] (" + type.name() + ")");
        list.add("===========================================");
        list.add("Listing Specifications...");
        list.add("- Ram: " + type.ram() + "MB");
        list.add("- CPU Specifications:");
        list.add("  Base Frequency: " + type.processor().baseFrequency());
        list.add("  Max Frequency: " + type.processor().maxFrequency());
        list.add("  Cores: " + type.processor().cores());
        list.add("  Threads: " + type.processor().threads());
        list.add("===========================================");
        list.add("If you're ready to go, type 'push'.");
        return list;
    }
}

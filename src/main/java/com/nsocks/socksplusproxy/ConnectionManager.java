package com.nsocks.socksplusproxy;

import io.netty.channel.Channel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

//TODO: synchronize cleanly using ReentrantLock?
//TODO: make all methods nullable and have handlers close unaccounted for channels (parent and child).
public class ConnectionManager {
    public static final ConnectionManager DEFAULT = new ConnectionManager();

    private Map<Channel, Map<Integer, Channel>> childChannelsForParent = new ConcurrentHashMap<>();
    private Map<Channel, Pair<Integer, Channel>> childChannelIdsAndParents = new ConcurrentHashMap<>();

    private ConnectionManager() {
    }

    public Channel getChildChannel(Channel parentChannel, Integer childChannelId) {
        Map<Integer, Channel> channelsForParent = childChannelsForParent.get(parentChannel);
        if (channelsForParent == null) {
            throw new IllegalStateException("ParentChannel not registered");
        }
        Channel childChannel = channelsForParent.get(childChannelId);
        if (childChannel == null) {
            throw new NoSuchElementException("ChildChannel structure with id " + childChannelId + " not found in Parent structure");
        }

        return childChannel;
    }

    public void addParentChannel(Channel parentChannel) {
        if (childChannelsForParent.putIfAbsent(parentChannel, new ConcurrentHashMap<>()) != null) {
            throw new IllegalStateException("ParentChannel already registered");
        }
    }

    public void removeParentChannel(Channel parentChannel) {
        Map<Integer, Channel> channelsForParent = childChannelsForParent.remove(parentChannel);
        if (channelsForParent == null) {
            throw new IllegalStateException("ParentChannel not registered");
        }
        int notFounds = 0;
        for (Channel childChannel : channelsForParent.values()) {
            if (childChannelIdsAndParents.remove(childChannel) == null) {
                notFounds++;
            }
        }
        if (notFounds > 0) {
            throw new IllegalStateException("ChildChannels not found. total: " + notFounds);
        }
    }

    public void addChildChannel(Channel parentChannel, Integer childChannelId, Channel childChannel) throws Exception {
        Map<Integer, Channel> parentChannelMap = childChannelsForParent.get(parentChannel);
        if (parentChannelMap == null) {
            throw new NoSuchElementException("ParentChannel structure not found");
        }
        if (childChannelIdsAndParents.putIfAbsent(childChannel, Pair.of(childChannelId, parentChannel)) != null) {
            throw new IllegalStateException("ChildChannel already registered");
        }
        if (parentChannelMap.putIfAbsent(childChannelId, childChannel) != null) {
            throw new IllegalStateException("ChildChannel with Id " + childChannelId + " already registered for Parent");
        }
    }

    public void removeChildChannel(Channel childChannel) {
        Pair<Integer, Channel> childChannelIdAndParent = childChannelIdsAndParents.remove(childChannel);
        if (childChannelIdAndParent == null) {
            throw new IllegalStateException("ChildChannel not registered");
        }
        Channel parentChannel = childChannelIdAndParent.getRight();
        Map<Integer, Channel> parentChannelMap = childChannelsForParent.get(parentChannel);
        if (parentChannelMap == null) {
            throw new NoSuchElementException("ParentChannel structure not found");
        }
        Integer childChannelId = childChannelIdAndParent.getLeft();
        if (parentChannelMap.remove(childChannelId) == null) {
            throw new NoSuchElementException("ChildChannel structure with id " + childChannelId + " not found in Parent structure");
        }
    }

    public Channel removeChildChannel(Channel parentChannel, Integer connectionId) {
        Map<Integer, Channel> parentChannelMap = childChannelsForParent.get(parentChannel);
        if (parentChannelMap == null) {
            throw new NoSuchElementException("ParentChannel structure not found");
        }
        Channel childChannel = parentChannelMap.remove(connectionId);
        if (childChannel == null) {
            throw new NoSuchElementException("ChildChannel structure with id " + connectionId + " not found in Parent structure");
        }
        if (childChannelIdsAndParents.remove(childChannel) == null) {
            throw new IllegalStateException("ChildChannel not registered");
        }
        return childChannel;
    }
}

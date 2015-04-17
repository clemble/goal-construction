package com.clemble.casino.goal.construction.listener;

import com.clemble.casino.lifecycle.initiation.InitiationState;
import com.clemble.casino.goal.lifecycle.initiation.GoalInitiation;
import com.clemble.casino.goal.construction.repository.GoalInitiationRepository;
import com.clemble.casino.server.event.goal.SystemGoalInitiationDueEvent;
import com.clemble.casino.server.event.goal.SystemGoalStartedEvent;
import com.clemble.casino.server.player.notification.SystemEventListener;
import com.clemble.casino.server.player.notification.SystemNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mavarazy on 9/19/14.
 */
public class SystemGoalInitiationDueEventListener implements SystemEventListener<SystemGoalInitiationDueEvent>{

    final private Logger LOG = LoggerFactory.getLogger(SystemGoalInitiationDueEvent.class);

    final private GoalInitiationRepository initiationRepository;
    final private SystemNotificationService notificationService;

    public SystemGoalInitiationDueEventListener(SystemNotificationService notificationService, GoalInitiationRepository initiationRepository) {
        this.notificationService = notificationService;
        this.initiationRepository = initiationRepository;
    }

    @Override
    public void onEvent(SystemGoalInitiationDueEvent event) {
        LOG.debug("1. Fetching related initiation");
        GoalInitiation initiation = initiationRepository.findOne(event.getGoalKey());
        LOG.debug("1.1 Updating initiation state");
        initiation = initiation.copyWithState(InitiationState.initiated);
        LOG.debug("1.2 Saving new initiation");
        initiationRepository.save(initiation);
        LOG.debug("2. Notifying of goal started event, for manager processing");
        notificationService.send(new SystemGoalStartedEvent(initiation.getGoalKey(), initiation));
    }

    @Override
    public String getChannel() {
        return SystemGoalInitiationDueEvent.CHANNEL;
    }

    @Override
    public String getQueueName() {
        return SystemGoalInitiationDueEvent.CHANNEL + " > goal:initiation";
    }
}

package com.clemble.casino.goal.construction.service;

import com.clemble.casino.error.ClembleCasinoError;
import com.clemble.casino.error.ClembleCasinoException;
import com.clemble.casino.goal.GoalAware;
import com.clemble.casino.goal.construction.GoalKeyGenerator;
import com.clemble.casino.goal.lifecycle.configuration.GoalConfiguration;
import com.clemble.casino.goal.lifecycle.construction.service.GoalConstructionService;
import com.clemble.casino.lifecycle.construction.ConstructionState;
import com.clemble.casino.goal.lifecycle.construction.GoalConstruction;
import com.clemble.casino.goal.lifecycle.construction.GoalConstructionRequest;
import com.clemble.casino.goal.construction.repository.GoalConstructionRepository;
import com.clemble.casino.money.Money;
import com.clemble.casino.payment.service.PlayerAccountService;
import com.clemble.casino.server.ServerService;
import com.clemble.casino.server.event.SystemEvent;
import com.clemble.casino.server.event.goal.SystemGoalStartedEvent;
import com.clemble.casino.server.event.payment.SystemPaymentFreezeRequestEvent;
import com.clemble.casino.server.player.notification.SystemNotificationService;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by mavarazy on 9/10/14.
 */
public class ServerGoalConstructionService implements GoalConstructionService, ServerService {

    final private Logger LOG = LoggerFactory.getLogger(GoalConstructionService.class);

    final private GoalKeyGenerator keyGenerator;
    final private SystemNotificationService notificationService;
    final private PlayerAccountService accountService;
    final private GoalConstructionRepository constructionRepository;

    public ServerGoalConstructionService(
        GoalKeyGenerator keyGenerator,
        PlayerAccountService accountService,
        SystemNotificationService notificationService,
        GoalConstructionRepository constructionRepository) {
        this.keyGenerator = keyGenerator;
        this.notificationService = notificationService;
        this.constructionRepository = constructionRepository;
        this.accountService = accountService;
    }

    @Override
    public GoalConstruction construct(GoalConstructionRequest request) {
        throw new UnsupportedOperationException();
    }

    public GoalConstruction construct(String player, GoalConstructionRequest request) {
        if (!(request.getConfiguration() instanceof GoalConfiguration))
            throw new IllegalArgumentException();//TODO add specific error for that
        // Step 1. Checking this is appropriate request for this service
        if (request.getGoal() == null || request.getGoal().isEmpty())
            throw ClembleCasinoException.fromError(ClembleCasinoError.GoalIsEmpty, player);
        // Step 1.1. Checking there is enough money to complete it
        Money price = request.getConfiguration().getBet().getAmount();
        if (!accountService.canAfford(Collections.singleton(player), price.getCurrency(), price.getAmount()).isEmpty()){
            throw ClembleCasinoException.fromError(ClembleCasinoError.PaymentTransactionInsufficientMoney, player);
        }
        String goalKey = keyGenerator.generate(player);
        // Step 2. Creating new GoalConstruction
        MDC.put(GoalAware.GOAL_KEY, goalKey);
        LOG.debug("1. Creating new");
        GoalConstruction construction = request.toConstruction(player, goalKey);
        construction = construction.clone(ConstructionState.constructed);
        // Step 3. Saving game construction
        LOG.debug("2. Saving new");
        GoalConstruction savedConstruction =  constructionRepository.save(construction);
        // Step 4. Returning saved construction
        LOG.debug("3. Freezing amount for a player");
        SystemEvent freezeRequest = SystemPaymentFreezeRequestEvent.create(goalKey, player, price);
        notificationService.send(freezeRequest);
        LOG.debug("4. Notifying of goal started event, for manager processing");
        notificationService.send(new SystemGoalStartedEvent(goalKey, savedConstruction));
        LOG.debug("5. done");
        return savedConstruction;
    }

    @Override
    public Collection<GoalConstruction> getPending(String player) {
        return constructionRepository.findByPlayerAndState(player, ConstructionState.pending);
    }

}

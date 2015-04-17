package com.clemble.casino.goal.construction.service;

import com.clemble.casino.goal.GoalAware;
import com.clemble.casino.goal.lifecycle.configuration.GoalConfiguration;
import com.clemble.casino.goal.lifecycle.construction.service.GoalConstructionService;
import com.clemble.casino.lifecycle.construction.ConstructionState;
import com.clemble.casino.error.ClembleCasinoError;
import com.clemble.casino.error.ClembleCasinoException;
import com.clemble.casino.goal.lifecycle.construction.GoalConstruction;
import com.clemble.casino.goal.lifecycle.construction.GoalConstructionRequest;
import com.clemble.casino.goal.construction.GoalKeyGenerator;
import com.clemble.casino.goal.construction.repository.GoalConstructionRepository;
import com.clemble.casino.money.Money;
import com.clemble.casino.payment.service.PlayerAccountService;
import com.clemble.casino.server.event.goal.SystemGoalInitiationStartedEvent;
import com.clemble.casino.server.logging.LoggingInterceptor;
import com.clemble.casino.server.player.notification.SystemNotificationService;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by mavarazy on 9/10/14.
 */
public class SelfGoalConstructionService implements GoalConstructionService {

    final private Logger LOG = LoggerFactory.getLogger(GoalConstructionService.class);

    final private GoalKeyGenerator keyGenerator;
    final private SystemNotificationService notificationService;
    final private GoalConstructionRepository constructionRepository;
    final private PlayerAccountService accountService;

    public SelfGoalConstructionService(
        GoalKeyGenerator keyGenerator,
        SystemNotificationService notificationService,
        GoalConstructionRepository constructionRepository,
        PlayerAccountService accountService) {
        this.keyGenerator = keyGenerator;
        this.accountService = accountService;
        this.notificationService = notificationService;
        this.constructionRepository = constructionRepository;
    }

    @Override
    public GoalConstruction construct(GoalConstructionRequest request) {
        throw new UnsupportedOperationException();
    }

    // TODO move all construction checks in one place on level higher
    public GoalConstruction construct(String player, @Valid GoalConstructionRequest request) {
        // TODO remove when done
        if (!(request.getConfiguration() instanceof GoalConfiguration))
            throw new IllegalArgumentException();//TODO add specific error for that
        // Step 1. Checking this is appropriate request for this service
        if (request.getGoal() == null || request.getGoal().isEmpty())
            throw ClembleCasinoException.fromError(ClembleCasinoError.GoalIsEmpty, player);
        // Step 1.1. Checking there is enough money to complete it
        Money price = request.getConfiguration().getBet().getAmount();
        if (!accountService.canAfford(Collections.singleton(player), price.getCurrency(), price.getAmount()).isEmpty()){
            throw ClembleCasinoException.fromError(ClembleCasinoError.GameConstructionInsufficientMoney, player);
        }
        String goalKey = keyGenerator.generate(player);
        // Step 2. Creating new GoalConstruction
        MDC.put(GoalAware.GOAL_KEY, goalKey);
        LOG.debug("1. creating new");
        GoalConstruction construction = request.toConstruction(player, goalKey);
        construction = construction.clone(ConstructionState.constructed);
        // Step 3. Saving game construction
        LOG.debug("2. saving new");
        GoalConstruction savedConstruction =  constructionRepository.save(construction);
        // Step 4. Initiating saved session right away
        LOG.debug("3. sending initiation request");
        notificationService.send(new SystemGoalInitiationStartedEvent(savedConstruction.getGoalKey(), savedConstruction.toInitiation()));
        // Step 5. Returning saved construction
        LOG.debug("4. done");
        return savedConstruction;
    }

    @Override
    public Collection<GoalConstruction> getPending(String player) {
        return null;
    }

}

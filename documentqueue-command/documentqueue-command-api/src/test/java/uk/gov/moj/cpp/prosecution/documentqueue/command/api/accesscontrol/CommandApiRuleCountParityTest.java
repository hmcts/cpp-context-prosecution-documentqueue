package uk.gov.moj.cpp.prosecution.documentqueue.command.api.accesscontrol;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;

/**
 * J17/J25 parity characterisation (J17 golden master), DQ-M01 / BC-20.
 *
 * Deny assertions in access-control tests also pass when the kbase loads no rules at all, so pin exactly which
 * rules the COMMAND_API kbase loads.
 */
class CommandApiRuleCountParityTest {

    @Test
    void dqM01_commandApiKbaseShouldLoadAllFourAccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("COMMAND_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Update Document Status",
                "Attach Document",
                "delete documents of cases",
                "delete expired documents"));
    }
}

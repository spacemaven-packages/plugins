package net.derfruhling.spm.consumer

import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails

class SpmInfoDisambiguationRule implements AttributeDisambiguationRule<Boolean> {
    @Override
    void execute(MultipleCandidatesDetails<Boolean> booleanMultipleCandidatesDetails) {
        for(final def it in booleanMultipleCandidatesDetails.candidateValues) {
            if(it) {
                booleanMultipleCandidatesDetails.closestMatch(it)
                return
            }
        }

        booleanMultipleCandidatesDetails.closestMatch(false)
    }
}

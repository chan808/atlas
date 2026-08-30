package com.projectatlas.inquiry.application;

import com.projectatlas.inquiry.domain.BrainDump;
import com.projectatlas.inquiry.domain.ClarificationProposal;

public interface ClarificationProposalPort {

	ClarificationProposal propose(BrainDump brainDump);

}

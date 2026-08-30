package com.projectatlas.inquiry.infrastructure;

import org.springframework.stereotype.Component;

import com.projectatlas.inquiry.application.ClarificationProposalPort;
import com.projectatlas.inquiry.domain.BrainDump;
import com.projectatlas.inquiry.domain.ClarificationProposal;

@Component
class DeterministicClarificationProposalAdapter implements ClarificationProposalPort {

	private static final String QUESTION =
			"이 호기심에서 먼저 얻고 싶은 것은 원리를 설명할 수 있는 이해인가요, "
					+ "작은 예제로 직접 확인하는 경험인가요?";
	private static final String REASON =
			"답에 따라 다음 질문이 개념의 경계를 좁힐지, 만들거나 진단할 상황을 "
					+ "좁힐지가 달라지고 이후에 필요한 증거도 달라집니다.";

	@Override
	public ClarificationProposal propose(BrainDump brainDump) {
		return new ClarificationProposal(
				QUESTION,
				REASON,
				"deterministic-fake-v1",
				"clarification-question-v1");
	}

}

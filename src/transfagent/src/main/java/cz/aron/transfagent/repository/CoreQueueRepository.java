package cz.aron.transfagent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.CoreQueue;

public interface CoreQueueRepository extends JpaRepository<CoreQueue, Long> {

	@EntityGraph(attributePaths = {"apuSource"})
    CoreQueue findFirstByOrderById();
	
	@EntityGraph(attributePaths = {"apuSource"})
	List<CoreQueue> findFirst1000ByOrderById();

}

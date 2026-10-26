package org.springboot.troubleticketapi.repository;

import org.springboot.troubleticketapi.entity.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRepository extends JpaRepository<NoteEntity, String> {}

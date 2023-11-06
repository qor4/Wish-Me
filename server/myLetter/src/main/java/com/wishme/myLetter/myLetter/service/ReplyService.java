package com.wishme.myLetter.myLetter.service;

import com.wishme.myLetter.myLetter.domain.MyLetter;
import com.wishme.myLetter.myLetter.domain.Reply;
import com.wishme.myLetter.myLetter.dto.request.SaveReplyRequestDto;
import com.wishme.myLetter.myLetter.repository.MyLetterRepository;
import com.wishme.myLetter.myLetter.repository.ReplyRepository;
import com.wishme.myLetter.user.domain.User;
import com.wishme.myLetter.user.repository.UserRepository;
import com.wishme.myLetter.util.AES256;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReplyService {

    @Value("${key.AES256_Key}")
    String key;

    private final UserRepository userRepository;
    private final MyLetterRepository myLetterRepository;
    private final ReplyRepository replyRepository;

    @Transactional
    public Long saveReply(SaveReplyRequestDto saveReplyRequestDto, Authentication authentication) throws Exception {
        // 여기서 저장할 때 @OneToOne 이여서 같은 편지에 대해 답장을 중복해서 보내면 exception 되는거 처리 (근데 2번은 되는거 수정)
        MyLetter myLetter = myLetterRepository.findByMyLetterSeq(saveReplyRequestDto.getMyLetterSeq())
                .orElseThrow(() -> new EmptyResultDataAccessException("해당 편지는 존재하지 않습니다. 해당 편지에 답장을 쓸 수 없습니다.", 1));

        if(myLetter.getToUser().getUserSeq() != Long.parseLong(authentication.getName())) {
            throw new IllegalArgumentException("해당 유저는 답장을 보낼 수 없습니다.");
        }

        User toUser = userRepository.findByUserSeq(myLetter.getFromUser())
                .orElseThrow(() -> new EmptyResultDataAccessException("해당 유저는 존재하지 않습니다. 해당 유저에게 답장을 쓸 수 없습니다.", 1));

        AES256 aes256 = new AES256(key);
        String cipherContent = aes256.encrypt(saveReplyRequestDto.getContent());

        Reply reply = Reply.builder()
                .myLetter(myLetter)
                .toUser(toUser)
                .content(cipherContent)
                .fromUserNickname(myLetter.getToUser().getUserNickname())
                .color(saveReplyRequestDto.getColor())
                .fromUser(myLetter.getToUser())
                .build();

        return replyRepository.save(reply).getReplySeq();
    }
}

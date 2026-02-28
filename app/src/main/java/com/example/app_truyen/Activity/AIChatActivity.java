package com.example.app_truyen.Activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app_truyen.Adapters.AdapterChat;
import com.example.app_truyen.Models.Comment;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIChatActivity extends AppCompatActivity {
    private RecyclerView rvChat;
    private EditText edtMessage;
    private ImageView btnSend;
    private AdapterChat adapterChat;
    private List<Comment> listMessages;
    private String currentUserId;
    private String danhSachTruyenCuaApp = "Hiện tại app chưa tải xong dữ liệu truyện.";
    private static final String API_KEY = "AIzaSyC_e2wumIpgkBXURiVo4QEFFlIPGcLBIys";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private OkHttpClient client = new OkHttpClient();
    private HashMap<String, Story> storyMap = new HashMap<>();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        rvChat = findViewById(R.id.rvChat);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        ImageView imgBack = findViewById(R.id.imgBack);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "GUEST_" + UUID.randomUUID().toString();
        }

        listMessages = new ArrayList<>();
        adapterChat = new AdapterChat(this, listMessages, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(adapterChat);

        fetchStoriesForAI();

        addAIMessage("Chào đạo hữu! Ta là Khí Linh cai quản Tàng Kinh Các. Đạo hữu cần ta đề xuất truyện hay giải thích kiến thức tu tiên gì nào?", null);

        imgBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String question = edtMessage.getText().toString().trim();
            if (!question.isEmpty()) {
                sendMessageToAI(question);
            }
        });
    }

    private void sendMessageToAI(String question) {
        Comment userMsg = new Comment(
                UUID.randomUUID().toString(),
                currentUserId,
                "Bạn",
                "",
                question,
                new ArrayList<>(),
                Timestamp.now()
        );
        listMessages.add(userMsg);
        adapterChat.notifyItemInserted(listMessages.size() - 1);
        rvChat.smoothScrollToPosition(listMessages.size() - 1);

        edtMessage.setText("");
        btnSend.setEnabled(false);

        try {
            JSONObject part = new JSONObject();
            String promptMat = "Ngươi là Khí Linh thông thái của ứng dụng truyện. " +
                    "Hãy luôn xưng hô là 'ta' và gọi người dùng là 'đạo hữu'.\n" +
                    "DỮ LIỆU TRUYỆN (V.Tổng: Lượt xem tất cả, V.Tháng: Xem tháng này, V.Tuần: Xem tuần này):\n" +
                    danhSachTruyenCuaApp + "\n\n" +
                    "NHIỆM VỤ MỚI:\n" +
                    "1. Nếu đạo hữu hỏi về truyện 'Hot', 'Xem nhiều', 'Bảng xếp hạng', hãy nhìn vào các chỉ số V.Tổng, V.Tháng hoặc V.Tuần để tư vấn bộ có số cao nhất.\n" +
                    "2. Khi giới thiệu, hãy nêu kèm số lượt xem để tăng độ uy tín. Ví dụ: 'Bộ này đang rất hot với 1.2K lượt xem trong tháng này'.\n" +
                    "LUẬT BẮT BUỘC: Luôn đính kèm thẻ [ID_TRUYEN:mã_id] ở cuối mỗi bộ truyện được nhắc đến để ta hiển thị khung hình.\n" +
                    "Câu hỏi của đạo hữu: " + question;

            part.put("text", promptMat);

            JSONArray parts = new JSONArray();
            parts.put(part);
            JSONObject content = new JSONObject();
            content.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(content);
            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        addAIMessage("Khí Linh đang bị nhiễu sóng, không thể kết nối mạng. Đạo hữu vui lòng thử lại sau!", null);
                        btnSend.setEnabled(true);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray candidates = jsonObject.getJSONArray("candidates");
                            String aiReply = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

                            runOnUiThread(() -> {
                                String cleanReply = aiReply.replace("*", "");

                                List<String> listAttachedIds = new ArrayList<>();
                                Pattern pattern = Pattern.compile("\\[ID_TRUYEN:(.*?)\\]");
                                Matcher matcher = pattern.matcher(cleanReply);

                                while (matcher.find()) {
                                    listAttachedIds.add(matcher.group(1).trim());
                                }

                                cleanReply = cleanReply.replaceAll("\\[ID_TRUYEN:(.*?)\\]", "").trim();

                                if (listAttachedIds.isEmpty()) {
                                    addAIMessage(cleanReply, null);
                                } else {
                                    addAIMessage(cleanReply, listAttachedIds.get(0));

                                    for (int i = 1; i < listAttachedIds.size(); i++) {
                                        addAIMessage("Đạo hữu có thể xem thêm bộ này nữa nhé:", listAttachedIds.get(i));
                                    }
                                }
                                btnSend.setEnabled(true);
                            });

                        } catch (Exception e) {
                            runOnUiThread(() -> { addAIMessage("Ta đang tẩu hỏa nhập ma!", null); btnSend.setEnabled(true); });
                        }
                    } else {
                        // XỬ LÝ KHI CÓ LỖI TỪ GOOGLE
                        String errorBody = "";
                        try {
                            if (response.body() != null) {
                                errorBody = response.body().string();
                            }
                        } catch (Exception ignored) {}

                        final String finalError = errorBody;
                        final int errorCode = response.code();

                        runOnUiThread(() -> {
                            addAIMessage("Lỗi từ Google (Code " + errorCode + "):\n" + finalError, null);
                            btnSend.setEnabled(true);
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addAIMessage(String text, String attachedStoryId) {
        Comment aiMsg = new Comment(
                UUID.randomUUID().toString(),
                "AI_BOT_123",
                "Khí Linh AI",
                "https://cdn-icons-png.flaticon.com/512/4712/4712035.png",
                text,
                new ArrayList<>(),
                Timestamp.now()
        );

        // NẾU CÓ MÃ ID -> LẤY ẢNH VÀ TÊN TỪ BỘ NHỚ ĐẮP VÀO COMMENT
        if (attachedStoryId != null && storyMap.containsKey(attachedStoryId)) {
            Story s = storyMap.get(attachedStoryId);
            aiMsg.setStoryIdDinhKem(s.getMaTruyen());
            aiMsg.setTenTruyenDinhKem(s.getTenTruyen());
            aiMsg.setAnhTruyenDinhKem(s.getAnhBiaUrl());
        }

        listMessages.add(aiMsg);
        adapterChat.notifyItemInserted(listMessages.size() - 1);
        rvChat.smoothScrollToPosition(listMessages.size() - 1);
    }

    private void fetchStoriesForAI() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("Truyen")
                .limit(1000)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    storyMap.clear();

                    StringBuilder sb = new StringBuilder("DATA APP [ID | Tên | Thể loại | Tác giả | V.Tổng | V.Tháng | V.Tuần]\n");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Story story = doc.toObject(Story.class);
                            story.setMaTruyen(doc.getId());
                            storyMap.put(story.getMaTruyen(), story);

                            String theLoaiStr = (story.getTheLoai() != null) ? String.join(",", story.getTheLoai()) : "Khác";
                            String tacGia = (story.getTacGia() != null && !story.getTacGia().isEmpty()) ? story.getTacGia() : "Ẩn danh";

                            // Lấy số lượt xem từ model
                            int vAll = story.getViewCountAll();
                            int vMonth = story.getViewCountMonth();
                            int vWeek = story.getViewCountWeek();

                            // Nén dữ liệu cực gọn để AI dễ đọc bảng xếp hạng
                            sb.append("[")
                                    .append(story.getMaTruyen()).append("|")
                                    .append(story.getTenTruyen()).append("|")
                                    .append(theLoaiStr).append("|")
                                    .append(tacGia).append("|")
                                    .append(vAll).append("|")
                                    .append(vMonth).append("|")
                                    .append(vWeek)
                                    .append("]\n");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    danhSachTruyenCuaApp = sb.toString();
                });
    }
}
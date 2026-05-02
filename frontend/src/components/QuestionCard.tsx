import { Link } from "react-router-dom";
import { ThumbsUp, Eye, MessageCircle } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { formatRelativeTime } from "@/lib/utils";
import type { QuestionSummary } from "@/types";

const diffColors: Record<string, string> = {
  EASY: "bg-green-100 text-green-800 border-green-200",
  MEDIUM: "bg-amber-100 text-amber-800 border-amber-200",
  HARD: "bg-red-100 text-red-800 border-red-200",
};

export function QuestionCard({ q }: { q: QuestionSummary }) {
  return (
    <Link to={`/questions/${q.id}`}>
      <Card className="hover:shadow-md transition-shadow">
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <div className="flex flex-col items-center gap-1 pt-1 text-muted-foreground">
              <ThumbsUp className="h-4 w-4" />
              <span className="text-xs font-medium">{q.likeCount}</span>
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-sm font-semibold leading-snug line-clamp-2">{q.title}</h3>
              <div className="flex flex-wrap items-center gap-2 mt-2">
                <Badge variant="outline" className="text-[10px]">{q.topicName}</Badge>
                <Badge variant="outline" className={`text-[10px] ${diffColors[q.difficulty] || ""}`}>
                  {q.difficulty}
                </Badge>
                {q.tags?.slice(0, 3).map((tag) => (
                  <Badge key={tag} variant="secondary" className="text-[10px]">{tag}</Badge>
                ))}
              </div>
              <div className="flex items-center gap-4 mt-2 text-xs text-muted-foreground">
                <span className="flex items-center gap-1"><Eye className="h-3 w-3" />{q.viewCount}</span>
                <span className="flex items-center gap-1"><MessageCircle className="h-3 w-3" />{q.answerCount}</span>
                <span>by {q.authorUsername}</span>
                <span>{formatRelativeTime(q.createdAt)}</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

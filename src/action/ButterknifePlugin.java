package action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlFile;
import entity.Element;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import utils.Util;
import view.FindViewByIdDialog;

import java.util.ArrayList;
import java.util.List;


public class ButterknifePlugin extends AnAction {

    private FindViewByIdDialog mDialog;
    private String xmlFileName;

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        //1.获取用户选择的layout的名字
        //获取project对象
        Project project = event.getProject();
        //获取编辑区对象
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if(null == editor){
            return;
        }
        //获取用户选择的字符
        SelectionModel model = editor.getSelectionModel();
        xmlFileName = model.getSelectedText();
        //如果用户没有选择正确，就弹出一个对话框
        if(TextUtils.isEmpty(xmlFileName)){
            //获取光标所在的位置的对应的布局文件
            xmlFileName = getCurrentLayout(editor);
            if(TextUtils.isEmpty(xmlFileName)){
                //弹一个对话框让用户自己输入
                xmlFileName= Messages.showInputDialog(project,"请输入layout名","未输入",Messages.getInformationIcon());
                if(TextUtils.isEmpty(xmlFileName)){
                    Util.showPopupBalloon(editor,"用户没有输入layout",5);
                    return;
                }
            }
        }
        //如果程序能运行到这个位置，就表示已经得到了layout名字了
        //2.找到对应的XML文件并把XML文件中所有的ID都获取到并记录下来（保存到集合中)
        //PSI文件  Program Sturcture Interface
        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, xmlFileName + ".xml", GlobalSearchScope.allScope(project));
        if(psiFiles.length<=0){
            Util.showPopupBalloon(editor,"未找到选中的布局文件"+xmlFileName,5);
            return;
        }
        //如果找到了XML文件，就去得到这个文件对应的PSI对象
        XmlFile xmlFile = (XmlFile) psiFiles[0];
        //解析xml文件 jdom dom4j pull
        List<Element> elements=new ArrayList<>();
        //得到布局文件中的所有ID，并存入elements集合中
        Util.getIDsFromLayout(xmlFile,elements);

        //3.生成UI，并根据用户使用UI时的选择生成代码
        if(elements.size()!=0){
            //得到当前编辑框对应的java文件所对应的psiFile对象 （mainActivity）
            PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
            // 根据psiFile对象生成对应的psiClass对象  这个对象用于后期把代码加入到mainActivity中
            PsiClass psiClass = Util.getTargetClass(editor, psiFile);
            //根据XML的内容生成UI，代码最后的生成就是UI的按钮事件中完成
            mDialog=new FindViewByIdDialog(editor,project,psiFile,psiClass,elements,xmlFileName);
            mDialog.showDialog();
        }
    }

    private String getCurrentLayout(Editor editor) {
        Document document = editor.getDocument();
        //取到插字光标模式对象
        CaretModel caretModel = editor.getCaretModel();
        //得到光标的位置
        int offset = caretModel.getOffset();
        //得到一行开始和结束的地方
        int lineNumber = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int lineEndOffset = document.getLineEndOffset(lineNumber);
        //获取一行内容
        String lineContent = document.getText(new TextRange(lineStartOffset, lineEndOffset));
        String layoutMatching = "R.layout.";
        if(!TextUtils.isEmpty(lineContent) && lineContent.contains(layoutMatching)){
            //获取layout文件的字符串
            int startPosition = lineContent.indexOf(layoutMatching + layoutMatching.length());
            int endPosition = lineContent.indexOf(")", startPosition);
            String layoutStr = lineContent.substring(startPosition,endPosition);
            return layoutStr;
        }
        return null;
    }


}

package Chat;

// README
// RODAR NO LINUX
// 

import java.awt.Color;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import pnp.UpnpExample;

interface Constantes{
    public static final int portaEnvio=6991;
    public static final int portaEscuto=6991;
}

public class ChatFrame extends javax.swing.JFrame  {
    static Object lockListaPeers = new Object();

    static String meuNick;
    
    String MASTER_IP; 
    
    static int ID;
     
    static Vector<Peer> listaPeers = new Vector<Peer>();
    
    public static Boolean chatIsON;
    static Boolean server;
    Boolean redeInterna ;
    
    public ChatFrame(String meuNick , String ip , Boolean server , Boolean redeInterna){
        chatIsON = true;
        this.redeInterna=redeInterna;
        MASTER_IP = ip;
        this.server = server;
        initComponents();
        ClienteEscutaPorta(Constantes.portaEscuto);
        
        this.meuNick= meuNick;
        ID = meuNick.hashCode();
        jLabel1.setText(meuNick);
        this.listaPeers = enviarMensagemServidor();
        atualizarlistarOnlines();
        jLabel1.setText(meuNick+" "+listaPeers.get(searchNome_index(meuNick)).PEER_IP);
    }
    
    synchronized public static void retornoParaHistorico(String string){
        String[] t =string.split(Pattern.quote (":"));
      
        if(t[0].equals("Eu falo para")){
            ChatFrame.historico.setForeground(Color.RED); 
        }else{
            ChatFrame.historico.setForeground(Color.BLUE);
        } 
        historico.append(string+"\n");
    }
    
    public void ClienteEscutaPorta(int portaPC){
        if(!redeInterna){
            Thread clienteAbrePortaRoteador = new UpnpExample( 6991 , portaPC);
            clienteAbrePortaRoteador.start();
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                System.out.println("Thread.sleep" + 
                        " IOException: " + e);
            }
        }
        Thread clienteEscutaPorta = new ClienteEscutaOutros(portaPC);
        clienteEscutaPorta.start();
    }
    
    public  Vector<Peer> enviarMensagemServidor(){
        String mensagemEnviar = "MASTER_PEER CONNECT " + meuNick +"\n\n";
        ClienteEnvia enviaBuscaPeers =new ClienteEnvia(MASTER_IP, Constantes.portaEnvio, mensagemEnviar, 0);
        return enviaBuscaPeers.clienteBuscaNovosPeers();
    }
    
    public static  String[] listarOnlines(){
        if(listaPeers!=null){
            List listUsersOnline =new ArrayList(); 
            for(int i=0 ; i< listaPeers.size() ; i++){
                if(!listaPeers.get(i).PEER_NAME.equals(meuNick) && listaPeers.get(i).PEER_STATUS)
                   listUsersOnline.add(listaPeers.get(i).PEER_NAME);
            }
            String[] strings = (String[]) listUsersOnline.toArray (new String[listUsersOnline.size()]);
            return strings;
        }
        return null; 
    }
   
     
    public static String tratarUpdate(){
        String resposta = "MASTER_PEER UPDATE ";
        for(int i=0 ; i< ChatFrame.listaPeers.size() ; i++){
                 resposta +=("("+ ChatFrame.listaPeers.get(i).PEER_ID +","+  ChatFrame.listaPeers.get(i).PEER_NAME +","
                                        + ChatFrame.listaPeers.get(i).PEER_IP +","+ ChatFrame.listaPeers.get(i).PEER_STATUS +","+
                                         ChatFrame.listaPeers.get(i).PEER_KEY+")"+'\n');
        }
        resposta +="\n";
        return resposta;
    }
    
    synchronized public static void atualizarlistarOnlines(){
        String mensagem = tratarUpdate();
        if(server){
            for(int i=0 ; i < listaPeers.size() ; i++){
               if(!meuNick.equals(listaPeers.get(i).PEER_NAME)){
                    Thread msgEnvia = new ClienteEnviaUpdate( listaPeers.get(i).PEER_IP , mensagem,
                                                               Constantes.portaEnvio ,
                                                                ID );
                    
                    msgEnvia.start();
               }
            }
        }
        
        listaPeersOnline.setModel(new javax.swing.AbstractListModel() {
                String[] strings = listarOnlines();
                public int getSize() { return strings.length; }
                public Object getElementAt(int i) { return strings[i]; }
            });
    }
    
    public void enviar() throws InterruptedException{
        
            if(!textoEnviar.getText().isEmpty() && listaPeers!=null){
               List<String> listUsersEnviar = new ArrayList<String>();
               listUsersEnviar =  listaPeersOnline.getSelectedValuesList();

               for(int i=0 ; i < listUsersEnviar.size() ; i++){
                   Thread msgEnvia = new ClienteEnvia( listaPeers.get(searchNome_index(listUsersEnviar.get(i)) ).PEER_IP , Constantes.portaEnvio ,
                                               textoEnviar.getText() , ID );
                   msgEnvia.start();
               }

               textoEnviar.setText("");
           }
        
    }
    
    public static String searchID_NAME(int ID ){
        for(int i = 0; i< listaPeers.size() ; i++){
            if(listaPeers.get(i).PEER_ID == ID)
                return listaPeers.get(i).PEER_NAME;
        }
        return null;
    }
    
    public static int searchIP_ID(String ip){
        for(int i=0 ; i< listaPeers.size() ; i++){
            if(listaPeers.get(i).PEER_IP == ip)
                return listaPeers.get(i).PEER_ID;
        }
        return 0;

    }
     public static int searchIP_index(String ip){
        if(listaPeers!=null){
            for(int i=0 ; i< listaPeers.size() ; i++){
                if(listaPeers.get(i).PEER_IP.equals(ip))
                    return i;
            }
        }
        return -1;
    }
    public static String searchIP_NAME(String ip){
        for(int i=0 ; i< listaPeers.size() ; i++){
            if(listaPeers.get(i).PEER_IP.equals(ip))
                return listaPeers.get(i).PEER_NAME;
        }
        return null;

    }
    public static int searchNome_index(String nome){
        if(listaPeers!=null){
            for(int i=0 ; i< listaPeers.size() ; i++){
                if(listaPeers.get(i).PEER_NAME.equals(nome))
                    return i;
            }
        }
        return -1;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        enviar = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        historico = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listaPeersOnline = new javax.swing.JList();
        textoEnviar = new javax.swing.JTextField();
        desconectar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        enviar.setBackground(new java.awt.Color(0, 255, 51));
        enviar.setText("Enviar");
        enviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enviarActionPerformed(evt);
            }
        });
        enviar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                enviarKeyTyped(evt);
            }
            public void keyPressed(java.awt.event.KeyEvent evt) {
                enviarKeyPressed(evt);
            }
        });

        historico.setEditable(false);
        historico.setColumns(20);
        historico.setRows(5);
        jScrollPane3.setViewportView(historico);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        listaPeersOnline.setModel(new javax.swing.AbstractListModel() {
            String[] strings = listarOnlines();

            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }

        });
        jScrollPane1.setViewportView(listaPeersOnline);

        textoEnviar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textoEnviarKeyPressed(evt);
            }
        });

        desconectar.setText("Sair");
        desconectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                desconectarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(textoEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(enviar, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(desconectar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(20, 20, 20))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 3, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textoEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(enviar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(desconectar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(20, 20, 20))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void enviarKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_enviarKeyTyped
        // TODO add your handling code here:

    }//GEN-LAST:event_enviarKeyTyped

    private void enviarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_enviarKeyPressed
        // TODO add your handling code here:

    }//GEN-LAST:event_enviarKeyPressed
    
    
    private void enviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enviarActionPerformed
        try {
            enviar();
        } catch (InterruptedException ex) {
            Logger.getLogger(ChatFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_enviarActionPerformed

    private void textoEnviarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textoEnviarKeyPressed
        if(evt.getKeyChar() == '\n'){
            try {
                enviar();
            } catch (InterruptedException ex) {
                Logger.getLogger(ChatFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_textoEnviarKeyPressed

    private void desconectarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_desconectarActionPerformed
           ClienteEnvia msgEnvia = new ClienteEnvia(MASTER_IP , Constantes.portaEnvio ,"Desconectar", ID );
           msgEnvia.desconectar();
           ChatFrame.chatIsON=false;
           this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_desconectarActionPerformed


   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton desconectar;
    private javax.swing.JButton enviar;
    public static javax.swing.JTextArea historico;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private static javax.swing.JList listaPeersOnline;
    private javax.swing.JTextField textoEnviar;
    // End of variables declaration//GEN-END:variables
    
}
